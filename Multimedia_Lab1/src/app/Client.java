
package app;

import gui.Gui;

import org.gstreamer.Gst;

/**
 * 
 * @author marc
 * This Application consists of a GUI that controls a gstreamer pipeline to connect to a server.
 * 
 */

public class Client {

	public static void main(String[] args){
		args = Gst.init("SWTMultimediaVideo", args);
		//create and init a gui
		Gui gui = new Gui();
		gui.init();
		//run the gui
		gui.run();
	}
}