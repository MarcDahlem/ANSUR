package client.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import server.connectionManager.ConnectionMessages;

public class RecorderHost {
	private int port;
	private String hostName;
	private String roomName;
	private String cameraName;

	public RecorderHost(String hostName, int port, String roomName, String cameraName) {
		this.port = port;
		this.hostName=hostName;
		this.roomName=roomName;
		this.cameraName = cameraName;
	}

	public int getPort(){
		return this.port;
	}

	public String getHostName(){
		return this.hostName;
	}
	
	public String getRoom(){
		return this.roomName;
	}
	
	public String getCameraName(){
		return this.cameraName;
	}

	public int getRemotePipelinePort() throws UnknownHostException, IOException {
		PrintWriter pw = null;
		Scanner scanner = null;
		Socket socket = null;
		try {
			//creates a socket and all the streams
			socket = new Socket(this.getHostName(), this.getPort());
			InputStream in = socket.getInputStream();
			OutputStream out = socket.getOutputStream();
			pw = new PrintWriter(out);
			
			//first try to get the socket. Like defined in the procol send also the own properties
			pw.write(ConnectionMessages.CAM_CONNECT_GET_PORT.name()+"\n");
			pw.write(this.getRoom() + "\n");
			pw.write(this.getCameraName() + "\n");
			pw.flush();

			//then wait for an answer and check if its the port or an error
			scanner = new Scanner(in);
			if (!scanner.hasNext()) {
				throw new IOException("Server did not answer!");
			}
			if (!scanner.hasNextInt()) {
				String answer = "";
				if (scanner.hasNextLine()) {
				answer = scanner.nextLine();
				}else {
					while (scanner.hasNext()) {
						answer = answer + " " + scanner.next();
					}
				}
				
				if (ConnectionMessages.valueOf(scanner.next()) != ConnectionMessages.SERVER_EXCEPTION) {
					//unknown answer received
					throw new IOException("Server didn't answer correct. Answer was '" + answer + "'.");
				} else {
					//EXCEPTION. Means that the server
					throw new IOException("Room '" + this.getRoom() + "' has already a camera with the name '" + this.getCameraName() + "'. Please rename it. Connection aborted.");
				}
			}
			
			//everything is fine. Read the port and return it
			int remote_port = scanner.nextInt();
			return remote_port;
		} finally {
			//finally close all streams etc
			if(pw!=null) {

				pw.close();
			}
			if (scanner!=null) {
				scanner.close();	
			}
			if (socket!=null){
				socket.close();
			}
		}
	}
}
