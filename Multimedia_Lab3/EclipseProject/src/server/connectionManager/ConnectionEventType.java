package server.connectionManager;

public enum ConnectionEventType {
	/** Camera connection
	 * camera tries to connect. Start a new port and send this port back
	 * paramaters (line)
	 * 	1: Room name
	 *  2: Camera name
	 *  
	 *  returns port on which the new pipe is created
	 *  exception if camera already existing in this room //TODO (not implemented at the moment)
	 */
	CAM_CONNECT_GET_PORT,

	
	/** Client Registration
	 * a new android client registers its Google Cloud Messages ID. Returns true or false
	 * parameters (line)
	 * 	1: GCM ID
	 * returns true if successfully registered, false if something went wrong (id already registered)
	 */
	CLIENT_REGISTER,
	
	/** Client deregistration
	 * a android client wants to deregister
	 * parameters (line)
	 * 	1: GCM ID
	 * returns true if successfully deregistered, false else
	 */
	CLIENT_DEREGISTER,
	
	/** Client request for cams notidications
	 * a android client wants to get all connected cameras
	 * parameters: no
	 * returns: (1) n number of cameras
	 * 			(2-(n+1)) camera (line-divided port room cameraname)
	 */
	CLIENT_GET_CAMS,
	
	/** Client want to subscribe for cams
	 * a android client wants to subscribe for a given number of cameras
	 * parameters (line)
	 * 	1: int n number of cameras connect to
	 * 	2: String google cloud message ID
	 *  3-(n+2): port of the cameras to them one wants to be notifed about
	 *  returns done if done
	 *  exception: if a camera is not available or the gcm is not valid.
	 */
	CLIENT_SUBSCRIBE,
	
	/**Client unsubscribption
	 * a android client wants to unsubscribe for some cameras and stop notifications for them
	 * parameters (line)
	 *   1: int n number of cameras to be unsubscibed
	 *   2: String Google Cloud Messaging ID
	 *   3-(n+2): camera ports to unsubscibe
	 *   returns done if done
	 *   exception if not available
	 */
	CLIENT_UNSUBSCRIBE,
	
	/** Exception message for the back way
	 *  
	 */
	SERVER_EXCEPTION
}
