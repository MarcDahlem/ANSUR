package commonUtility;

public class GcmMessages {

	public static String COMMAND = "com.example.stramwithvlc.message";
	
	public static String CAMERA_NAME = "com.example.stramwithvlc.content.camera_name";
	
	public static String PORT = "com.example.stramwithvlc.content.port";
	
	public static String FILE_PATH = "com.example.stramwithvlc.content.file_path";
	
	public enum GCMCOMMAND {
		MOTION_START,

		MOTION_END,

		CAMERA_DOWN,

		SERVER_SHUTDOWN;

	}
	
}
