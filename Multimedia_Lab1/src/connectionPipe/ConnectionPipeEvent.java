package connectionPipe;

import java.util.EventObject;

import org.gstreamer.GstObject;

public class ConnectionPipeEvent extends EventObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8802208251156015500L;
	private ConnectionPipeEventType eventType;
	private String message;
	private GstObject gstSource;

	public ConnectionPipeEvent(Object source, GstObject gstSource, ConnectionPipeEventType eventType, String message) {
		super(source);
		//set the message and event type
		this.message = message;
		this.eventType = eventType;
		this.gstSource = gstSource;
	}

	/**
	 * @return the eventType
	 */
	public ConnectionPipeEventType getEventType() {
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
