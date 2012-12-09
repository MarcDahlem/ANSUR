package motionRecorder;

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

	public MotionRecorderEvent(Object source, GstObject gstSource, MotionRecorderEventType eventType, String message) {
		super(source);
		//set the message and event type
		this.message = message;
		this.eventType = eventType;
		this.gstSource = gstSource;
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


}
