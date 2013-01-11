package server.connectionManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

import javax.swing.event.EventListenerList;

public class ConnectionManager {
	private int port;
	private ServerSocket serverSocket;
	private EventListenerList listeners;
	private int portCount = 5001;
	private volatile boolean stopped;

	public ConnectionManager(int port) throws IOException {
		this.port=port;
		this.serverSocket = new ServerSocket(this.port);
		this.listeners = new EventListenerList();
		this.stopped = false;
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
			default:
					//unnknown message received. //TODO
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

	private void clientRegistration(PrintWriter writer, Scanner scanner) throws IOException {
		//create a connection event and set the gcm address
		ConnectionEvent event = new ConnectionEvent(this, ConnectionEventType.CLIENT_REGISTER);
		if (!scanner.hasNextLine()) {
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
			throw new IOException("No room name given during the registration. Not conform to the specified protocol.");
		}
		String roomName = scanner.nextLine();
		if (!scanner.hasNextLine()) {
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
	}
}
