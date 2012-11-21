
package APP;

import org.gstreamer.Gst;
import org.gstreamer.swt.VideoComponent;

import GUI.Gui;
import Recorder.Recorder;
/**
 * 
 * @author marc
 * This Application consists of a GUI that controls a video recorder.
 * 
 */

public class MyRecorderApp {

	public static void main(String[] args){
		args = Gst.init("SWTMultimediaVideo", args);
		//create and init a gui
		Gui gui = new Gui();
		gui.init();
		//get the video component and create the recorder with it
		VideoComponent vid = gui.getVideoComponent();
		assert vid!=null;
		Recorder rec = new Recorder(vid);
		
		//set the recorder to be controlled by the gui
		gui.setRecorder(rec);
		//run the gui
		gui.run();
	}
}
