/**
 * 
 */
package Recorder;

import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.swt.VideoComponent;

/**
 * @author marc
 *
 */
public class Recorder {

	private Element pipe;
	private boolean isRecording;

	public Recorder(VideoComponent vid) {
		this.pipe = ElementFactory.make("camerabin", "cambin");

		Element src = ElementFactory.make("v4l2src", "video capturing source");
		this.pipe.set("video-source", src);




		Element sink = vid.getElement();
		sink.setName("SWTVideo");

		//TODO preference windows
		//Element enc = ElementFactory.make("x264enc", "avi Encoder");
		Element enc = ElementFactory.make("theoraenc", "Encoder ogg");
		//Element mux = ElementFactory.make("avimux", "avi Muxer");
		Element mux = ElementFactory.make("oggmux", "Ogg Muxer");

		this.pipe.set("viewfinder-sink", sink);
		this.pipe.set("video-encoder", enc);
		this.pipe.set("video-muxer", mux);
		this.pipe.set("mode", 1);
		this.pipe.set("mute", true);
	}

	public boolean isRecording() {
		return this.isRecording;
	}

	public void stopRec() {
		if (this.isRecording) {
			//recording => stop recording
			this.pipe.emit("capture-stop");
			this.isRecording = false;
		}
	}
	

	public void startRec(String fileName) {
		if (fileName == null) {
			throw new IllegalArgumentException("Filename to record to cannot be null");
		}

		if (!this.isRecording) {
			this.pipe.set("filename", fileName);
			this.pipe.emit("capture-start");
			this.isRecording=true;
		}

	}

	public void stop() {
		this.pipe.stop();
	}

	public void play() {
		this.pipe.play();
	}
}
