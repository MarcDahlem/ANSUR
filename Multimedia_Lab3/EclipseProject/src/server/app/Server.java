
package server.app;


import org.gstreamer.Gst;

import server.gui.Gui;


/**
 * 
 * @author marc
 * This Application consists of a GUI that controls a video recorder.
 * 
 */

public class Server {

	public static final String GCM_PROJECT_ID= "862106151827";
	public static final String GCM_SERVER_API_KEY="AIzaSyDYfvX2wGwRmUyeFVUKEKV1FYsA8rVVDaU";
	
	public static void main(String[] args){
		args = Gst.init("SWTMultimediaVideo", args);
		//create and init a gui
		Gui gui = new Gui();
		gui.init();
		//run the gui
		gui.run();
	}
}
