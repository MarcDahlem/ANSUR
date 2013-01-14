package server.motionRecorder;

import java.io.IOException;
import java.util.EventObject;

import org.gstreamer.GstObject;

public class MotionRecorderEvent extends EventObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8802208251156015500L;
	private MotionRecorderEventType eventType;
	private String message;
	private GstObject gstSource;
	private String fileName;
	private IOException exception;

	public MotionRecorderEvent(Object source, GstObject gstSource, MotionRecorderEventType eventType, String message, String fileName) {
		super(source);
		//set the message and event type
		this.message = message;
		this.eventType = eventType;
		this.gstSource = gstSource;
		this.fileName = fileName;
		this.exception=null;
	}

	/**
	 * @return the eventType
	 */
	public MotionRecorderEventType getEventType() {
		return this.eventType;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return this.message;
	}

	/**
	 * @return the gstSource
	 */
	public GstObject getGstSource() {
		return this.gstSource;
	}
	
	public String getFilePath(){
		return this.fileName;
	}

	/*
	 * only used if the pipeline event is CONNECTION_ERROR
	 * This exception is the eception thrown by the GCMManager while informing and should be handled in the GUI
	 */
	public void setException(IOException e) {
		this.exception=e;
	}
	
	/*
	 * only used if the pipeline event is CONNECTION_ERROR
	 * This exception is the eception thrown by the GCMManager while informing and should be handled in the GUI
	 */
	public IOException getException(){
		return this.exception;
	}


}
