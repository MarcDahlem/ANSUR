package server.connectionManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import javax.swing.event.EventListenerList;

import commonUtility.ConnectionEventType;

import server.motionRecorder.MotionRecorder;

public class ServerConnectionManager {
	private int port;
	private ServerSocket serverSocket;
	private EventListenerList listeners;
	private int portCount = 5001;
	private volatile boolean stopped;
	private List<ServerSocket> transportSockets;

	public ServerConnectionManager(int port) throws IOException {
		this.port=port;
		this.serverSocket = new ServerSocket(this.port);
		this.listeners = new EventListenerList();
		this.stopped = false;
		this.transportSockets=new LinkedList<ServerSocket>();
	}

	public void addConnectionListener( ConnectionListener listener ) {
		listeners.add(ConnectionListener.class, listener);
	}

	public void removeConnectionListener(ConnectionListener listener ) {
		listeners.remove(ConnectionListener.class, listener );
	}

	private synchronized void notifyConnectionEvent(ConnectionEvent event ) {
		for (ConnectionListener l : listeners.getListeners(ConnectionListener.class) ) {
			l.eventAppeared(event);
		}
	}

	public void start() throws IOException {
		Socket clientSocket;
		while (!this.stopped) {
			clientSocket=null;
			try{
				clientSocket = this.serverSocket.accept();
				handleConnection(clientSocket);
			} catch (SocketException e){
				if (!this.stopped) {
					throw e;
				} //else connection manager stopped, expected behavior
			} finally {
				if ( clientSocket != null ) {
					clientSocket.close();
				}
			}
		}
	}

	private void handleConnection(Socket client) throws IOException {
		InputStream in=null;
		OutputStream out=null;

		PrintWriter writer=null;
		Scanner scanner=null;

		try {
			in = client.getInputStream();
			out = client.getOutputStream();

			writer = new PrintWriter(out);
			scanner = new Scanner(in);

			String line = scanner.nextLine();

			ConnectionEventType message = ConnectionEventType.valueOf(line);
			switch(message) {
			case CAM_CONNECT_GET_PORT:
				// camera wants to connect... send back the port for this connection (where the pipeline will be started on)
				this.cam_connect_get_port(writer, scanner);
				break;
			case CLIENT_REGISTER:
				// a client wants to register its google cloud id on the server
				this.clientRegistration(writer, scanner);
				break;
			case CLIENT_DEREGISTER:
				// a client wants to deregister, and needs to be deleted from all notifications
				this.clientDeregistration(writer, scanner);
				break;
			case CLIENT_SUBSCRIBE:
				// a client wants to subscribe for some cameras
				this.clientSubscription(writer, scanner);
				break;
			case CLIENT_UNSUBSCRIBE:
				// a client wants to unsubscribe for some cameras
				this.clientUnsubscription(writer, scanner);
				break;
			case CLIENT_GET_CAMS:
				//a client wants to get a list with all the cameras
				this.clientGetAllCams(writer, scanner);
				break;
			case CLIENT_DOWNLOAD_MOTION:
				// a client wants to download a movie
				this.clientDownloadMotion(writer, out, scanner);
				break;
			default:
				//unknown message received. 
				writer.write(ConnectionEventType.SERVER_EXCEPTION+"\n");
				writer.flush();
				throw new IOException("Unknown command received: '" +message +"'.");
			}
		} finally {
			if (writer!= null) {
				writer.close();
			}
			if (scanner!= null) {
				scanner.close();
			}
			if (in!= null) {
				in.close();
			}
			if (out!= null) {
				out.close();
			}
		}
	}

	private void clientDownloadMotion(PrintWriter writer, OutputStream out, Scanner scanner) throws IOException {
		// client wants to download a movie. Send it back
		if (!scanner.hasNextLine()) {
			writer.write((ConnectionEventType.SERVER_EXCEPTION+"\n"));
			writer.flush();
			throw new IOException("No filename followed the download motion command. Not conform to the specified protocol.");
		}

		String filename = scanner.nextLine();

		//TODO checks

		final File motionFile = new File (filename);
		String motionFileName = motionFile.getName();
		final int fileLength = (int)motionFile.length();

		//write filelength, filename and the port where the transport will be started
		writer.write(fileLength+"\n");
		writer.write(motionFileName+"\n");
		final int transportPort = this.portCount;
		//start the connection on this new port
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					ServerConnectionManager.this.startMotionTransport(transportPort, motionFile, fileLength);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
			}
		}).start();
		
		this.portCount++;
		writer.write(transportPort+"\n");
		writer.flush();
	}

	public void startMotionTransport(int transportPort, File motionFile, int fileLength) throws IOException {
		ServerSocket downloadServerSocket = null;
		Socket transportSocket=null;
		
		try{
			downloadServerSocket = new ServerSocket(transportPort);
			this.transportSockets.add(downloadServerSocket);
			transportSocket = downloadServerSocket.accept();
			InputStream in=null;
			OutputStream out=null;
			BufferedInputStream bin=null;

			try {
				out = transportSocket.getOutputStream();
				byte [] bytearray  = new byte [fileLength];

				FileInputStream fileInputStream = new FileInputStream(motionFile);
				bin = new BufferedInputStream(fileInputStream);
				bin.read(bytearray,0,bytearray.length);
				//write motion file
				out.write(bytearray, 0, bytearray.length);
				out.flush();

			} finally {

				if (in!= null) {
					in.close();
				}
				if (out!= null) {
					out.close();
				}
				if (bin!=null) {
					bin.close();
				}
			}
		} catch (SocketException e){
			if (!this.stopped) {
				throw e;
			} //else connection manager stopped, expected behavior
		} finally {
			if ( transportSocket != null ) {
				transportSocket.close();
			}
			if (downloadServerSocket != null) {
				downloadServerSocket.close();
			}
			if (!stopped) {
				//do not delete if it was caused by a stop event
				this.transportSockets.remove(downloadServerSocket);
			}
		}
	}

	private void clientGetAllCams(PrintWriter writer, Scanner scanner) throws IOException {
		// a client wants to have all cameras registered
		if (!scanner.hasNextLine()) {
			writer.write(ConnectionEventType.SERVER_EXCEPTION+"\n");
			writer.flush();
			throw new IOException("No client id followed to the getallcameras command. Not conform to the specified protocol.");
		}
		//first get the clients registration id
		String regId = scanner.nextLine();

		//create the event and notify. After that should all motion recorders be added to the event and the names can be written to the writer
		ConnectionEvent event = new ConnectionEvent(this, ConnectionEventType.CLIENT_GET_CAMS);
		notifyConnectionEvent(event);
		ArrayList<MotionRecorder> cams = event.getAllCameras();

		//first write the amount of cameras
		writer.write(cams.size() + "\n");
		//then all the recorder names
		for (MotionRecorder cam : cams) {
			writer.write(cam.getPort() + "\n");
			writer.write(cam.getRoomName() + "\n");
			writer.write(cam.getCameraName() + "\n");
			writer.write(cam.getSubscriptionStatus(regId)+"\n");
		}

		writer.flush();
	}

	private void clientUnsubscription(PrintWriter writer, Scanner scanner) throws IOException {
		//create a connection event for every camera to unsubscribe
		if (!scanner.hasNextLine() || !scanner.hasNextInt()) {
			writer.write(ConnectionEventType.SERVER_EXCEPTION+"\n");
			writer.flush();
			throw new IOException("No number of cameras to unsubscribe followed to the unsubscribe command. Not conform to the specified protocol.");
		}

		if (!scanner.hasNextInt()) {
			writer.write(ConnectionEventType.SERVER_EXCEPTION+"\n");
			writer.flush();
			throw new IOException("No amount of cameras to unsubscribe followed to the unsubscribe command. Not conform to the specified protocol.");
		}

		int amount = scanner.nextInt();
		scanner.nextLine();

		if (!scanner.hasNextLine()) {
			writer.write(ConnectionEventType.SERVER_EXCEPTION+"\n");
			writer.flush();
			throw new IOException("No GCM id followed to the amount of cameras in the unsubscibe command. Not conform to the specified protocol.");
		}

		String gcm = scanner.nextLine();

		for (int i=0;i<amount;i++) {
			ConnectionEvent event = new ConnectionEvent(this, ConnectionEventType.CLIENT_UNSUBSCRIBE);
			event.setGCM(gcm);
			if (!scanner.hasNextLine()) {
				writer.write(ConnectionEventType.SERVER_EXCEPTION+"\n");
				writer.flush();
				throw new IOException("No camera number " + (i+1) + ". But " + amount + " cameras for the unsubscription defined. Not conform to the specified protocol.");
			}

			if(!scanner.hasNextInt()) {
				writer.write(ConnectionEventType.SERVER_EXCEPTION+"\n");
				writer.flush();
				throw new IOException("Camera number " + (i+1) + " is not a port number. Not conform to the specified protocol.");
			}

			int port = scanner.nextInt();
			scanner.nextLine();

			event.setEventPort(port);
			this.notifyConnectionEvent(event);
			if (event.hasError()) {
				//error appeard means gcm was already registered
				writer.write(ConnectionEventType.SERVER_EXCEPTION+"\n");
				writer.flush();
				throw new IOException("Camera number " + (i+1) + " is not valid or GCM not found. Unsubscription aborted");
			} 
		}

		writer.write("done\n");
		writer.flush();
	}

	private void clientSubscription(PrintWriter writer, Scanner scanner) throws IOException {
		//create a connection event for every camera to subscribe on
		if (!scanner.hasNextLine() || !scanner.hasNextInt()) {
			writer.write(ConnectionEventType.SERVER_EXCEPTION+"\n");
			writer.flush();
			throw new IOException("No number of cameras to subscribe followed to the subscribe command. Not conform to the specified protocol.");
		}

		if (!scanner.hasNextInt()) {
			writer.write(ConnectionEventType.SERVER_EXCEPTION+"\n");
			writer.flush();
			throw new IOException("No amount of cameras to subscribe followed to the subscribe command. Not conform to the specified protocol.");
		}

		int amount = scanner.nextInt();
		scanner.nextLine();

		if (!scanner.hasNextLine()) {
			writer.write(ConnectionEventType.SERVER_EXCEPTION+"\n");
			writer.flush();
			throw new IOException("No GCM id followed to the amount of cameras in the subscibe command. Not conform to the specified protocol.");
		}

		String gcm = scanner.nextLine();

		for (int i=0;i<amount;i++) {
			ConnectionEvent event = new ConnectionEvent(this, ConnectionEventType.CLIENT_SUBSCRIBE);
			event.setGCM(gcm);
			if (!scanner.hasNextLine()) {
				writer.write(ConnectionEventType.SERVER_EXCEPTION+"\n");
				writer.flush();
				throw new IOException("No camera number " + (i+1) + ". But " + amount + " cameras for the subscription defined. Not conform to the specified protocol.");
			}

			if(!scanner.hasNextInt()) {
				writer.write(ConnectionEventType.SERVER_EXCEPTION+"\n");
				writer.flush();
				throw new IOException("Camera number " + (i+1) + " is not a port number. Not conform to the specified protocol.");
			}

			int port = scanner.nextInt();
			scanner.nextLine();

			event.setEventPort(port);
			this.notifyConnectionEvent(event);
			if (event.hasError()) {
				//error appeard means gcm was already registered
				writer.write(ConnectionEventType.SERVER_EXCEPTION+"\n");
				writer.flush();
				throw new IOException("Camera number " + (i+1) + " is not valid or GCM not found. Subscription aborted");
			} 
		}

		writer.write("done\n");
		writer.flush();
	}

	private void clientDeregistration(PrintWriter writer, Scanner scanner) throws IOException {
		//create a connection event and set the gcm address
		ConnectionEvent event = new ConnectionEvent(this, ConnectionEventType.CLIENT_DEREGISTER);
		if (!scanner.hasNextLine()) {
			writer.write(ConnectionEventType.SERVER_EXCEPTION+"\n");
			writer.flush();
			throw new IOException("No google cloud message id followed to the deregistration command. Not conform to the specified protocol.");
		}

		String gcm = scanner.nextLine();
		event.setGCM(gcm);
		this.notifyConnectionEvent(event);

		if (event.hasError()) {
			//error appeard means gcm was already registered
			writer.write(""+false+"\n");
		} else {
			// no error thrown while notifying. That means room
			writer.write(""+true+"\n");
		} 
		writer.flush();
	}

	private void clientRegistration(PrintWriter writer, Scanner scanner) throws IOException {
		//create a connection event and set the gcm address
		ConnectionEvent event = new ConnectionEvent(this, ConnectionEventType.CLIENT_REGISTER);
		if (!scanner.hasNextLine()) {
			writer.write(ConnectionEventType.SERVER_EXCEPTION+"\n");
			writer.flush();
			throw new IOException("No google cloud message id followed to the registration command. Not conform to the specified protocol.");
		}

		String gcm = scanner.nextLine();
		event.setGCM(gcm);
		this.notifyConnectionEvent(event);

		if (event.hasError()) {
			//error appeard means gcm was already registered
			writer.write(""+false+"\n");
		} else {
			// no error thrown while notifying. That means room
			writer.write(""+true+"\n");
		} 
		writer.flush();
	}

	private void cam_connect_get_port(PrintWriter writer, Scanner scanner) throws IOException {
		ConnectionEvent event = new ConnectionEvent(this, ConnectionEventType.CAM_CONNECT_GET_PORT);
		event.setEventPort( this.portCount);
		if (!scanner.hasNextLine()) {
			writer.write(ConnectionEventType.SERVER_EXCEPTION+"\n");
			writer.flush();
			throw new IOException("No room name given during the registration. Not conform to the specified protocol.");
		}
		String roomName = scanner.nextLine();
		if (!scanner.hasNextLine()) {
			writer.write(ConnectionEventType.SERVER_EXCEPTION+"\n");
			writer.flush();
			throw new IOException("No camera name given for the registration of a camera. Not conform to the specified protocol.");
		}

		String cameraName = scanner.nextLine();

		event.setRoomName(roomName);
		event.setCameraName(cameraName);

		this.notifyConnectionEvent(event);

		if (event.hasError()) {
			//error appeard means cameraname was already existent in this room
			writer.write(ConnectionEventType.SERVER_EXCEPTION.name() + "\n");
		} else {
			// no error thrown while notifying. That means room
			writer.write(""+this.portCount++);
		} 
		writer.flush();
	}

	public void stop() throws IOException{
		this.stopped = true;
		this.serverSocket.close();
		for (ServerSocket sock:this.transportSockets) {
			sock.close();
		}
		//delete all of this sockets
		this.transportSockets=new LinkedList<ServerSocket>();
	}
}
