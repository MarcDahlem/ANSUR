package APP;

import org.gstreamer.Gst;
import org.gstreamer.swt.VideoComponent;

import GUI.Gui;
import Recorder.Recorder;


public class MyRecorderApp {

	public static void main(String[] args){
		args = Gst.init("SWTMultimediaVideo", args);
		Gui gui = new Gui();
		gui.init();
		VideoComponent vid = gui.getVideoComponent();
		assert vid!=null;
		Recorder rec = new Recorder(vid);
		gui.setRecorder(rec);
		gui.run();
	}
}
