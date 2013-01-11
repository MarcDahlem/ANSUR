package client.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class RecorderHost {
	private int port;
	private String hostName;

	public RecorderHost(String hostName, int port) {
		this.port = port;
		this.hostName=hostName;
	}

	public int getPort(){
		return this.port;
	}

	public String getHostName(){
		return this.hostName;
	}

	public int getRemotePipelinePort() throws UnknownHostException, IOException {
		Socket socket = new Socket(this.getHostName(), this.getPort());
		InputStream in = socket.getInputStream();
		OutputStream out = socket.getOutputStream();
		PrintWriter pw = new PrintWriter(out);
		pw.write("get_port_and_start\n");
		pw.flush();
		Scanner scanner = new Scanner(in);
		if (!scanner.hasNextInt()) {
			throw new IOException("Server didn't answer correct. Answer was '" + (scanner.hasNext() ? scanner.next() : "No answer!") + "'.");
		}
		int remote_port = scanner.nextInt();
		pw.close();
		scanner.close();
		socket.close();
		return remote_port;
	}
}
