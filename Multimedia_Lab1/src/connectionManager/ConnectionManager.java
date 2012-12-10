package connectionManager;

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
			if (line.equals("get_port_and_start")) {
				//TODO start pipeline (listener to Gui?)
				ConnectionEvent event = new ConnectionEvent(this, ConnectionEventType.CLIENT_START, this.portCount);
				this.notifyConnectionEvent(event);
				writer.write(""+this.portCount++);
				writer.flush();
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

	public void stop() throws IOException{
		this.stopped = true;
		this.serverSocket.close();
	}
}
