package commonUtility;

public class GcmMessages {

	public static String COMMAND = "com.example.stramwithvlc.message";
	
	public static String CAMERA_NAME = "com.example.stramwithvlc.content.camera_name";
	
	public static String ROOM_NAME = "com.example.stramwithvlc.content.room_name";
	
	public static String PORT = "com.example.stramwithvlc.content.port";
	
	public static String FILE_PATH = "com.example.stramwithvlc.content.file_path";
	
	public enum GCMCOMMAND {
		/**event sent when a motion recording is started. File is not downloadable here. But it should be used as notificationid
		 * Informations set in GCM messages:
		 * 		FILE_PATH: for the notification id
		 * 		CAMERA_NAME: to display
		 * 		ROOM_NAME: to display
		 * 		PORT: port of the stream as ID
		 */
		MOTION_START,

		/**event sent when a motion recording is ended
		 * Informations set in GCM messages:
		 * 		FILE_PATH: to be able to download
		 * 		CAMERA_NAME: to display
		 * 		ROOM_NAME: to display
		 * 		PORT: port of the stream as ID
		 */
		MOTION_END,

		/**Event send if a camera is stopped and disconnected from the server
		 * Informations set in GCM messages:
		 * 		CAMERA_NAME: to identify cam
		 * 		ROOM_NAME: to identify room
		 * 		PORT: port of the stream as ID
		 */
		CAMERA_DOWN,

		/**Event if the server shuts down. Client need to set unregistered on server since the server does not save its state
		 * Informations set in GCM messages:
		 * none
		 */
		SERVER_SHUTDOWN;

	}
	
}
