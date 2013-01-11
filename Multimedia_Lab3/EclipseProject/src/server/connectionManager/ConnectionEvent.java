package server.connectionManager;

import java.util.EventObject;

public class ConnectionEvent extends EventObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private ConnectionEventType eventType;
	private int port;

	private String roomName;

	private String cameraName;

	private boolean errorAppeared;

	public ConnectionEvent(Object source, ConnectionEventType eventType, int eventPort) {
		super(source);
		//set the message and event type
		this.port = eventPort;
		this.eventType = eventType;
		this.errorAppeared=false;
	}

	/**
	 * @return the eventType
	 */
	public ConnectionEventType getEventType() {
		return this.eventType;
	}
	
	public void errorAppeared() {
		this.errorAppeared=true;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return this.port;
	}

	void setRoomName(String roomName) {
		this.roomName = roomName;
	}

	void setCameraName(String cameraName) {
		this.cameraName=cameraName;
	}
	
	public String getRoomName(){
		return this.roomName;
	}
	
	public String getCameraName(){
		return this.cameraName;
	}

	// returns if one of the listeners set the error flag.
	public boolean hasError() {
		return this.errorAppeared;
	}


}
