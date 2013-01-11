package server.connectionManager;

import java.util.EventObject;

public class ConnectionEvent extends EventObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private ConnectionEventType eventType;
	private int port;

	public ConnectionEvent(Object source, ConnectionEventType eventType, int portCount) {
		super(source);
		//set the message and event type
		this.port = portCount;
		this.eventType = eventType;
	}

	/**
	 * @return the eventType
	 */
	public ConnectionEventType getEventType() {
		return this.eventType;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return this.port;
	}


}
