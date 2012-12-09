package data;

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
}
