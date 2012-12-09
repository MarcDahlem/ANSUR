/**
 * 
 */
package motionRecorder;

import java.util.List;

import org.gstreamer.Bin;
import org.gstreamer.Bus;
import org.gstreamer.Closure;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Event;
import org.gstreamer.GhostPad;
import org.gstreamer.GstObject;
import org.gstreamer.Pad;
import org.gstreamer.Pad.EVENT_PROBE;
import org.gstreamer.Pipeline;
import org.gstreamer.event.EOSEvent;
import org.gstreamer.swt.VideoComponent;

/**
 * @author marc
 * 
 * This recorder is the server part of our application.
 * It can play and record video from connected client simultaniously.
 * The recording is started automatically when a motion in the client video is detected.
 * Thatfore it is required, that the 'motiondetector' plugin from <a href="https://github.com/codebrainz/motiondetector">https://github.com/codebrainz/motiondetector</a> is installed on the running instance
 * It records and plays at the moment only video and no sound, with the theora decoder/encoder and ogg de/muxer.
 * Stopped once, the pipe cannot be set back to running due to timestamp problems in the pipe. Either call {@link #init()} again or create a new pipe to restart it.
 */
public class MotionRecorder {

	/**
	 * The pipeline containing the camerabin and is used to control the recorder
	 */
	private Pipeline pipe;

	/**
	 * a boolean value that descibes, if the recorder is capturing video at the moment
	 */
	private boolean isRecording;

	private Bin firstBin;
	private Element switcher;

	private Bin currentRecordBin;

	private int port;

	private Bin currentPlayBin;

	/** The default constructor for this recorder.
	 * It will set the values of this auto-recording pipe.
	 * To initialize it use {@link #init()} and after that run the recorder with {@link #run()}
	 * It supports listeners that inform about pipeline changes like motion detected(aka recording started), bus errors, warnings etc.
	 * 
	 * @param port the port on which the server should listen for the incoming
	 */
	public MotionRecorder(int port) {
		this.port = port;
	}

	/**
	 * initializes the bin, but uses instead of playback a fakesink.
	 * To show the video call the method {@link #setPlayer(VideoComponent)}.
	 */
	public void init() {
		//create the main playback bin
		Bin firstBin = new Bin("firstpart");
		Pipeline pipe = new Pipeline("Webcam Recorder/Player on Servers port "+this.port);
		this.addBusListeners(pipe);

		//create the source of the video for the recorder. To the webcame is a motiondetector connected, but not used at the moment
		// requires a installed "motion" plugin in gstreamer

		Bin sourceBin = createSourceBin();
		Element tee = ElementFactory.make("tee", "Tee split buffer on port " + this.port);
		Element switcher = ElementFactory.make("valve", "Switcher for recording on port " + this.port);
		Element record_queue= ElementFactory.make ("queue", "recording queue on port " + this.port);
		switcher.set("drop", false);
		Bin fakeRecordBin = createFakeRecordBin();
		Bin playBin = createPlayBin();

		firstBin.addMany(sourceBin, tee, record_queue, switcher, playBin);
		Element.linkMany(sourceBin, tee);
		Element.linkMany(tee, record_queue, switcher);
		Element.linkMany(tee, playBin);
		// add a ghost pad, so that the bin is accessible from the outside
		Pad staticSourcePad = switcher.getStaticPad("src");
		GhostPad ghost = new GhostPad("src", staticSourcePad);
		firstBin.addPad(ghost);

		pipe.addMany(firstBin, fakeRecordBin);
		boolean connected = Element.linkMany(firstBin, fakeRecordBin);

		if (!connected) {
			throw new IllegalStateException("firstbin cannot be connected with first recording bin  on port " + this.port);
		}


		this.currentPlayBin = playBin;
		this.currentRecordBin=fakeRecordBin;
		this.switcher=switcher;
		this.firstBin = firstBin;
		this.pipe=pipe;
	}

	private void addBusListeners(Pipeline pipe) {
		pipe.getBus().connect(new Bus.ERROR() {

			@Override
			public void errorMessage(GstObject source, int code, String message) {
				// TODO Auto-generated method stub
				System.out.println("Error Server (" + source.getName() + "): " +message);
			}
		});

		pipe.getBus().connect(new Bus.INFO() {

			@Override
			public void infoMessage(GstObject source, int code, String message) {
				System.out.println("INFO Server (" + source.getName() + "): " + message);

			}
		});

		pipe.getBus().connect(new Bus.WARNING() {

			@Override
			public void warningMessage(GstObject source, int code, String message) {
				System.out.println("Warning Server (" + source.getName() + "): " + message);
			}
		});

		pipe.getBus().connect(new Bus.EOS() {

			@Override
			public void endOfStream(GstObject source) {
				System.out.println ("EOS received. Source: '" + source.getName() + "'.");
				MotionRecorder.this.stop();
			}
		});
	}

	private Bin createFakeRecordBin() {
		Bin recordBin=new Bin("Recorder fake subpipe on port " + this.port);
		Element fakeSink = ElementFactory.make("fakesink","Recorder fakesink on port " + this.port);
		recordBin.addMany(fakeSink);
		Pad staticSourcePad = fakeSink.getStaticPad("sink");
		GhostPad ghost = new GhostPad("sink", staticSourcePad);
		recordBin.addPad(ghost);

		return recordBin;
	}

	private Bin createRealRecordBin(String fileName) {
		// assert fileName != null
		Bin recordBin = new Bin("Recorder subpipe on port " + this.port);
		//create color space changer
		Element ffmpeg = ElementFactory.make("ffmpegcolorspace", "ffmpeg color space server recordbin on port " + this.port);
		//create encoder and muxer
		Element enc = ElementFactory.make("theoraenc", "Theora encoder on server on port " + this.port);
		Element mux = ElementFactory.make("oggmux", "ogg muxer on server on port " + this.port);
		// fileSink
		Element fileSink = ElementFactory.make("filesink", "File Sink on port " + this.port);
		fileSink.set("location", fileName);
		recordBin.addMany(ffmpeg, enc,  mux, fileSink);
		boolean connected = Element.linkMany(ffmpeg, enc, mux, fileSink);
		if (!connected) {
			throw new IllegalStateException("Linking of encoder muxer and fileSInk failed on port " + this.port);
		}
		// add a ghost pad, so that the bin is accessible from the outside
		Pad staticSourcePad = ffmpeg.getStaticPad("sink");

		GhostPad ghost = new GhostPad("sink", staticSourcePad);
		recordBin.addPad(ghost);

		return recordBin;
	}

	private Bin createPlayBin() {
		Bin playBin = new Bin("playback pipe on port " + this.port);
		Element vidSink = ElementFactory.make("fakesink", "fakesink for streamplayback on port " + this.port);

		Element play_queue= ElementFactory.make ("queue", "playback queue on port "+this.port);
		play_queue.set("leaky", 1);

		playBin.addMany(play_queue, vidSink);
		Element.linkMany(play_queue,vidSink);

		// add a ghost pad, so that the bin is accessible from the outside
		Pad staticSourcePad = play_queue.getStaticPad("sink");
		GhostPad ghost = new GhostPad("sink", staticSourcePad);
		playBin.addPad(ghost);

		return playBin;
	}

	private Bin createSourceBin() {
		Bin sourceBin = new Bin("source");
		Element src = ElementFactory.make("tcpserversrc", "tcpserversrc on port "+this.port);
		src.set("port", this.port);

		Element demux = ElementFactory.make("oggdemux", "Ogg demuxer on port " + this.port);
		final Element dec = ElementFactory.make("theoradec", "Theora decoder on port " + this.port);
		demux.connect(new Element.PAD_ADDED() {

			@Override
			public void padAdded(Element element, Pad pad) {
				Element.linkMany(element, dec);
				pipe.debugToDotFile(Pipeline.DEBUG_GRAPH_SHOW_NON_DEFAULT_PARAMS, "server_running_playback on port " + MotionRecorder.this.port);
			}
		});

		Element motionDetection = ElementFactory.make("motiondetector", "motion detection on port " + this.port);
		motionDetection.set("draw_motion", true);
		//add motion detection callbacks
		motionDetection.connect("notify::motion-detected", new Closure() {
			boolean motionStart = true;
			@SuppressWarnings("unused") //it is used from the JNA, therefore that it is set as callback
			public void invoke() {
				if (motionStart) {
					System.out.println("Motion start detected  on port " + MotionRecorder.this.port);
				}else {
					System.out.println("Motion end detected on port " + MotionRecorder.this.port);
				}
				motionStart = !motionStart;
			}
		});
		Element ffmpeg = ElementFactory.make("ffmpegcolorspace", "ffmpegcolorspace for the motion detection on port " + this.port);

		//link the motion detector and the webcam
		sourceBin.addMany(src, demux, dec, ffmpeg, motionDetection);
		Element.linkMany(src, demux);
		Element.linkMany(dec, ffmpeg, motionDetection);

		// add a ghost pad, so that the bin is accessible from the outside
		Pad staticSourcePad = motionDetection.getStaticPad("src");
		GhostPad ghost = new GhostPad("src", staticSourcePad);
		sourceBin.addPad(ghost);
		//return the created sourceBin
		return sourceBin;
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
	public void stopRec(boolean stopPipe) {
		if (this.isRecording()) {
			//recording => stop recording
			Bin newFakeBin = this.createFakeRecordBin();
			this.changeRecordBin(newFakeBin, stopPipe);
		} else {
			if (stopPipe) {
				this.pipe.stop();
			}
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
			//not recording --> start record;
			//create recorder bin to set the new filename
			Bin newRecordBin = this.createRealRecordBin(fileName);
			changeRecordBin(newRecordBin, false);
		}

	}

	private void changeRecordBin(final Bin newRecordBin, final boolean stopPipe) {

		final Element last = this.currentRecordBin.getElementsSorted().get(0);
		Pad lastPad = last.getStaticPad("sink");
		lastPad.addEventProbe(new Pad.EVENT_PROBE() {


			@Override
			public boolean eventReceived(Pad pad, Event event) {
				return MotionRecorder.this.handleEOSOnRecordBin(newRecordBin, last, pad, event, this, stopPipe);
			}
		});

		this.switcher.set("drop", true);
		Pad sinkPad = this.currentRecordBin.getStaticPad("sink");
		sinkPad.sendEvent(new EOSEvent());
	}

	/**
	 * stops the recording and the playing of the webcam video
	 */
	public void stop() {
		//stop everything
		this.stopRec(true);
	}

	/**
	 * starts the recorder. This will only play video, but not capturing video
	 */
	public void run() {
		this.pipe.play();
	}

	private boolean handleEOSOnRecordBin(Bin newRecordBin, Element last, Pad pad,
			Event event, EVENT_PROBE probe, boolean stopPipe) {
		if (event instanceof EOSEvent) {
			System.out.println("EOS received in '"+pad.getName()+"' on '" + last.getName() + "'.");
			boolean removed = MotionRecorder.this.pipe.remove(MotionRecorder.this.currentRecordBin);
			if (!removed) {
				throw new IllegalStateException("current record bin cannot be removed from the record pipe on port " + this.port);
			}
			MotionRecorder.this.currentRecordBin.stop();
			MotionRecorder.this.pipe.addMany(newRecordBin);
			boolean connected = Element.linkMany(MotionRecorder.this.firstBin, newRecordBin);
			if (!connected) {
				throw new IllegalStateException("can not link the firstBin with the new recorder bin on port " + this.port);
			}
			newRecordBin.play();
			MotionRecorder.this.switcher.set("drop", false);

			MotionRecorder.this.currentRecordBin = newRecordBin;
			//change the isRecording status
			MotionRecorder.this.isRecording=!MotionRecorder.this.isRecording;
			pad.removeEventProbe(probe);
			this.pipe.debugToDotFile(Pipeline.DEBUG_GRAPH_SHOW_NON_DEFAULT_PARAMS, "server_isrecording_"+this.isRecording+"_port_" + this.port);
			if (stopPipe) {
				this.pipe.stop();
			}
			return false;
		} else {
			System.out.println("Event received, that is not an eos in '"+pad.getName()+"' on '" + last.getName() + "'  on port " + this.port + ".");
			return true;
		}
	}
	
	public void setPlayer(VideoComponent vid) {
		final Element vidSink = vid.getElement();
		vidSink.setName("SWTVideo on port " + this.port);
		// delete the old player (normally fakesink) and connect the new player
		List<Element> elementsSorted = this.currentPlayBin.getElementsSorted();
		final Element last = elementsSorted.get(0);
		final Element secondLast = elementsSorted.get(1);
		Pad lastPad = last.getStaticPad("sink");
		int numElements = elementsSorted.size();
		//assert num elements >=2 (minimum queue and playback device)
		Element first = elementsSorted.get(numElements-1);
		//block the outgoiding pad of the first element (should be queue)
		final Pad firstPad = first.getStaticPad("src");
		lastPad.addEventProbe(new Pad.EVENT_PROBE() {


			@Override
			public boolean eventReceived(Pad pad, Event event) {
				return MotionRecorder.this.handleEOSOnPlayBin(vidSink, last, secondLast, pad, firstPad, event, this);
			}
		});
		firstPad.setBlocked(true);
		
		
		//get the next element and send an eos event on it (should normally be the playback device (fakesink or video)
		Element second = elementsSorted.get(numElements-2);
		Pad sinkPad = second.getStaticPad("sink");
		sinkPad.sendEvent(new EOSEvent());
	}

	protected boolean handleEOSOnPlayBin(Element vidSink, Element last, Element secondLast, Pad occuredPad, Pad blockedPad, Event event, EVENT_PROBE probe) {
		if (event instanceof EOSEvent) {
			System.out.println("EOS received in '"+occuredPad.getName() +"' on '" + last.getName() + "'.");
			boolean removed = this.currentPlayBin.remove(last);
			if (!removed) {
				throw new IllegalStateException("current play bin cannot be removed from the pipe on port " + this.port);
			}
			last.stop();
			this.currentPlayBin.addMany(vidSink);
			boolean connected = Element.linkMany(secondLast, vidSink);
			if (!connected) {
				throw new IllegalStateException("can not link the new vidSink on port " + this.port);
			}
			vidSink.play();
			blockedPad.setBlocked(false);
			occuredPad.removeEventProbe(probe);
			return false;
		} else {
			System.out.println("Event received, that is not an eos in '"+occuredPad.getName()+"' on '"+ last.getName() + "'.");
			return true;
		}
	}
}
