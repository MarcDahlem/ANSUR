package server.connectionManager;

import java.util.ArrayList;
import java.util.EventObject;

import server.motionRecorder.MotionRecorder;

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

	private String gcm;

	private ArrayList<MotionRecorder> recorderList;

	public ConnectionEvent(Object source, ConnectionEventType eventType) {
		super(source);
		//set the message and event type
		this.eventType = eventType;
		this.errorAppeared=false;
		this.recorderList=new ArrayList<MotionRecorder>();
	}
	
	void setEventPort(int eventPort) {
		this.port = eventPort;
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

	void setGCM(String gcm) {
		this.gcm=gcm;
	}
	
	public String getGCM(){
		return this.gcm;
	}

	ArrayList<MotionRecorder> getAllCameras() {
		// return all motion recorders that have been added to this event after a get all movie request
		return this.recorderList;
	}
	
	public void addMotionRecorder(MotionRecorder recorder) {
		this.recorderList.add(recorder);
	}


}
