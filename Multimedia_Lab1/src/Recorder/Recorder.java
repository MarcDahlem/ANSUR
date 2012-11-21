/**
 * 
 */
package Recorder;

import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Pipeline;
import org.gstreamer.swt.VideoComponent;

/**
 * @author marc
 * 
 * This recorder is based on the gstreamer camerabin plugin.
 * It can record, and play a video from the connected webcam simultaneously. 
 * It has a motiondetector connected to the source, but that detector is not used at the moment.
 * Thatfore it is required, that the 'motion' plugin from <a href="https://gitorious.org/gstreamer-motion-plugin/gstreamer-motion-plugin/">https://gitorious.org/gstreamer-motion-plugin/gstreamer-motion-plugin/</a> is installed on the running instance
 * 
 * The recorder can have the states PLAYING, STOPPED and PLAYING_AND_RECORDING.
 * It records at the moment only video and no sound, with the theora encoder and ogg muxer.
 */
public class Recorder {

	/**
	 * The pipeline containing the camerabin and is used to control the recorder
	 */
	private Pipeline pipe;

	/**
	 * a boolean value that descibes, if the recorder is capturing video at the moment
	 */
	private boolean isRecording;

	/**The default constructor for this recorder. It needs a VideoComponent to get the video playpack sink
	 * 
	 * @param vid component containing the playback sink for the recorder
	 */
	public Recorder(VideoComponent vid) {
		//create the main recorder, camerabin
		Element camBin = ElementFactory.make("camerabin", "cambin");

		//create the source of the video for the camerbin. To the webcame is a motiondetector connected, but not used at the moment
		// requires a installed "motion" plugin in gstreamer
		Pipeline subpipe = new Pipeline();
		Element src = ElementFactory.make("v4l2src", "video capturing source");
		Element motionDetection = ElementFactory.make("motion", "motion detection");

		//link the motion detector and the webcam
		//subpipe.addMany(src/*, motionDetection*/);
		//subpipe.link(src, motionDetection);

		// set the video source of the cambin element to this subpipe (not working at the moment). Setting nothing uses the default webcam without motion
		//TODO
		camBin.set("video-source", src);
		//camBin.set("video-source-filter",motionDetection);

		// setting the video sink to the element used in the given VideoComponent
		Element vidSink = vid.getElement();
		vidSink.setName("SWTVideo");

		//TODO preferences
		//set the encoder and muxer. cambin is not working with avi encoder. It seems that it can only handle fast (realtime) capturing because of small buffers.
		//Element enc = ElementFactory.make("x264enc", "avi Encoder");
		Element enc = ElementFactory.make("theoraenc", "Encoder ogg");
		//Element mux = ElementFactory.make("avimux", "avi Muxer");
		Element mux = ElementFactory.make("oggmux", "Ogg Muxer");

		//set all the created elements in the cambin
		camBin.set("viewfinder-sink", vidSink);
		camBin.set("video-encoder", enc);
		camBin.set("video-muxer", mux);
		//seting video capturing mode in cambin (mode 0 /default would be picture snapshots)
		camBin.set("mode", 1);

		//disable audio at the moment
		camBin.set("mute", true);

		//create the main pipeline
		this.pipe = new Pipeline ();
		this.pipe.addMany(camBin);
	}

	/*
	 * returns, if the recorder is recording at the moment
	 */
	public boolean isRecording() {
		return this.isRecording;
	}

	/**
	 * stops the video recording
	 * If not recording nothing happens.
	 */
	public void stopRec() {
		if (this.isRecording) {
			//recording => stop recording
			Element cambin = this.pipe.getElementByName("cambin");
			//send the stop signal to the cambin
			cambin.emit("capture-stop");
			//set the recording status to false
			this.isRecording = false;
		}
	}

	/**
	 * Starts recording on the given filename. If the file is existent, it will overwrite it without asking!
	 * This method will not check the given ending.
	 * If the recorder is already recording, has this method no effect.
	 * @param fileName used to capture the data to
	 * @throws {@link java.lang.IllegalArgumentException} if the given filename is null.
	 */
	public void startRec(String fileName) {
		//check if the filename is null
		if (fileName == null) {
			throw new IllegalArgumentException("Filename to record to cannot be null");
		}

		// check if it is already recording
		if (!this.isRecording()) {
			//set the new filename
			Element cambin = this.pipe.getElementByName("cambin");
			cambin.set("filename", fileName);
			//and send the capture start signal to cambin
			cambin.emit("capture-start");
			//change the isRecording status
			this.isRecording=true;
		}

	}

	/**
	 * stops the recording and the playing of the webcame video
	 */
	public void stop() {
		//stop everything
		this.stopRec();
		this.pipe.stop();
	}

	/**
	 * starts the recorder. This will only play video, but not capturing video
	 */
	public void play() {
		this.pipe.play();
	}
}
