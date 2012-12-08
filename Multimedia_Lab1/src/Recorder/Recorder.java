/**
 * 
 */
package Recorder;

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
import org.gstreamer.lowlevel.GstDateTimeAPI;
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

	private Bin firstBin;
	private Element switcher;

	private Bin currentRecordBin;

	/**The default constructor for this recorder. It needs a VideoComponent to get the video playpack sink
	 * 
	 * @param vid component containing the playback sink for the recorder
	 */
	public Recorder(VideoComponent vid) {
		//create the main playback bin
		Bin firstBin = new Bin("firstpart");
		Pipeline pipe = new Pipeline("Webcam Recorder/Player on Server");
		this.addBusListeners(pipe);

		//create the source of the video for the recorder. To the webcame is a motiondetector connected, but not used at the moment
		// requires a installed "motion" plugin in gstreamer

		Bin sourceBin = createSourceBin();
		Element tee = ElementFactory.make("tee", "Tee split buffer");
		Element switcher = ElementFactory.make("valve", "Switcher for recording");
		Element record_queue= ElementFactory.make ("queue", "recording queue");
		switcher.set("drop", false);
		Bin fakeRecordBin = createFakeRecordBin();
		Bin playBin = createPlayBin(vid);

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
			throw new IllegalStateException("firstbin cannot be connected with first recording bin");
		}


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
				Recorder.this.stop();
			}
		});
	}

	private Bin createFakeRecordBin() {
		Bin recordBin=new Bin("Recorder fake subpipe");
		Element fakeSink = ElementFactory.make("fakesink","Recorder fakesink");
		recordBin.addMany(fakeSink);
		Pad staticSourcePad = fakeSink.getStaticPad("sink");
		GhostPad ghost = new GhostPad("sink", staticSourcePad);
		recordBin.addPad(ghost);

		return recordBin;
	}

	private Bin createRealRecordBin(String fileName) {
		// assert fileName != null
		Bin recordBin = new Bin("Recorder subpipe");
		//create color space changer
		Element ffmpeg = ElementFactory.make("ffmpegcolorspace", "ffmpeg color space server recordbin");
		//create encoder and muxer
		Element enc = ElementFactory.make("theoraenc", "Theora encoder on server");
		Element mux = ElementFactory.make("oggmux", "ogg muxer on server");
		// fileSink
		Element fileSink = ElementFactory.make("filesink", "File Sink");
		fileSink.set("location", fileName);
		recordBin.addMany(ffmpeg, enc,  mux, fileSink);
		boolean connected = Element.linkMany(ffmpeg, enc, mux, fileSink);
		if (!connected) {
			throw new IllegalStateException("Linking of encoder muxer and fileSInk failed");
		}
		// add a ghost pad, so that the bin is accessible from the outside
		Pad staticSourcePad = ffmpeg.getStaticPad("sink");

		GhostPad ghost = new GhostPad("sink", staticSourcePad);
		recordBin.addPad(ghost);

		return recordBin;
	}

	private Bin createPlayBin(VideoComponent vid) {
		Bin playBin = new Bin("playback");
		Element vidSink = vid.getElement();
		vidSink.setName("SWTVideo");

		Element play_queue= ElementFactory.make ("queue", "playback queue");
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
		Element src = ElementFactory.make("tcpserversrc", "tcpserversrc");
		src.set("port", 5000);
		//Pad pad = src.getStaticPad("src");
		//		pad.addEventProbe(new Pad.EVENT_PROBE() {
		//
		//			@Override
		//			public boolean eventReceived(Pad pad, Event event) {
		//				System.out.println("Event received: '" + event + "' on pad '" + pad.getName()+"'.");
		//				if (event instanceof EOSEvent) {
		//					//Recorder.this.pipe.stop();
		//					return false;
		//				} else {
		//					return false;
		//				}
		//			}
		//		});

		Element demux = ElementFactory.make("oggdemux", "Ogg demuxer");
		final Element dec = ElementFactory.make("theoradec", "Theora decoder");
		demux.connect(new Element.PAD_ADDED() {

			@Override
			public void padAdded(Element element, Pad pad) {
				Element.linkMany(element, dec);
				pipe.debugToDotFile(Pipeline.DEBUG_GRAPH_SHOW_NON_DEFAULT_PARAMS, "server_running_playback");
			}
		});

		Element motionDetection = ElementFactory.make("motiondetector", "motion detection");
		motionDetection.set("draw_motion", true);
		//add motion detection callbacks
		motionDetection.connect("notify::motion-detected", new Closure() {
			boolean motionStart = true;
			@SuppressWarnings("unused") //it is used from the JNA, therefore that it is set as callback
			public void invoke() {
				if (motionStart) {
					System.out.println("Motion start detected");
				}else {
					System.out.println("Motion end detected");
				}
				motionStart = !motionStart;
			}
		});
		Element ffmpeg = ElementFactory.make("ffmpegcolorspace", "ffmpegcolorspace");

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
			//			final Element fileSink = this.currentRecordBin.getElementByName("File Sink");
			//			this.switcher.set("drop", true);
			//			Pad pad = fileSink.getStaticPad("sink");
			//			pad.addEventProbe(new Pad.EVENT_PROBE() {
			//
			//				@Override
			//				public boolean eventReceived(Pad pad, Event event) {
			//					if (event instanceof EOSEvent) {
			//						System.out.println("EOS received in filesink");
			//						fileSink.stop();
			//						pad.removeEventProbe(this);
			//						return false;
			//					} else {
			//						System.out.println("Event received, that is not an eos on fileSink");
			//						return true;
			//					}
			//				}
			//			});
			//			pad.sendEvent(new EOSEvent());
			//			this.isRecording=false;
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
			//not recroding --> start record;
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
				return Recorder.this.receivedEOSOnRecordBin(newRecordBin, last, pad, event, this, stopPipe);
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
	public void play() {
		this.pipe.play();
	}

	private boolean receivedEOSOnRecordBin(final Bin newRecordBin, final Element last, Pad pad,
			Event event, EVENT_PROBE probe, boolean stopPipe) {
		if (event instanceof EOSEvent) {
			System.out.println("EOS received in '"+pad.getName()+"' on '" + last.getName() + "'.");
			boolean removed = Recorder.this.pipe.remove(Recorder.this.currentRecordBin);
			if (!removed) {
				throw new IllegalStateException("current record bin cannot be removed from the record pipe");
			}
			Recorder.this.currentRecordBin.stop();
			Recorder.this.pipe.addMany(newRecordBin);
			boolean connected = Element.linkMany(Recorder.this.firstBin, newRecordBin);
			if (!connected) {
				throw new IllegalStateException("can not link the firstBin with the new recorder bin");
			}
			newRecordBin.play();
			Recorder.this.switcher.set("drop", false);

			Recorder.this.currentRecordBin = newRecordBin;
			//change the isRecording status
			Recorder.this.isRecording=!Recorder.this.isRecording;
			pad.removeEventProbe(probe);
			this.pipe.debugToDotFile(Pipeline.DEBUG_GRAPH_SHOW_NON_DEFAULT_PARAMS, "server_isrecording_"+this.isRecording);
			if (stopPipe) {
				this.pipe.stop();
			}
			return false;
		} else {
			System.out.println("Event received, that is not an eos in '"+pad.getName()+"' on '" + last.getName() + "'.");
			return true;
		}
	}
}
