/**
 * 
 */
package server.motionRecorder;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.event.EventListenerList;

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

import server.connectionManager.GCMManager;

import commonUtility.GcmMessages;

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

	//private static final String FILESAFE_MUXER = "theoraenc";
	//private static final String FILESAFE_MUXER = "ffenc_mpeg4";

	//private static final String FILESAFE_ENCODER = "oggmux";
	//private static final String FILESAFE_ENCODER = "mp4mux";

	public static final String FILEENDING = "ogg";

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

	private EventListenerList listeners;

	private boolean stopped;

	private String path;

	private String roomName;

	private String camerName;

	private TreeSet<String> registeredGCMs;

	/** The default constructor for this recorder.
	 * It will set the values of this auto-recording pipe.
	 * To initialize it use {@link #init()} and after that run the recorder with {@link #run()}
	 * It supports listeners that inform about pipeline changes like motion detected(aka recording started), bus errors, warnings etc.
	 * 
	 * @param port the port on which the server should listen for the incoming
	 * @param roomName the name of the room where the camera is in
	 * @param cameraName the name of the camera in the given room
	 * @throws IllegalArgumentException if the given recording path is null or the port <1 or >99999
	 */
	public MotionRecorder(int port, String roomName, String cameraName, String path) {
		if (path==null || port<1 || port>99999 || roomName == null || cameraName == null) {
			throw new IllegalArgumentException();
		}
		this.port = port;
		this.roomName = roomName;
		this.camerName = cameraName;
		this.listeners = new EventListenerList();
		this.registeredGCMs = new TreeSet<String>();
		this.stopped=false;
		String fileSeperator = System.getProperty("file.separator");
		if (path.endsWith(fileSeperator)) {
			this.path = path;
		} else {
			this.path = path+fileSeperator;
		}
	}

	public void addMotionRecorderListener( MotionRecorderListener listener ) {
		listeners.add(MotionRecorderListener.class, listener);
	}

	public void removeMotionRecorderListener( MotionRecorderListener listener ) {
		listeners.remove( MotionRecorderListener.class, listener );
	}

	private synchronized void notifyPipelineEvent(MotionRecorderEvent event ) {
		for (MotionRecorderListener l : listeners.getListeners(MotionRecorderListener.class) ) {
			l.eventAppeared(event);
			System.out.println(event.getEventType().name() + " (" + event.getGstSource().getName() + "): " +event.getMessage());
		}

		//inform all subscribed apps
		switch (event.getEventType()) {
		case MOTION_START: {
			Map<String, String> data = new TreeMap<String,String>();
			data.put(GcmMessages.COMMAND, GcmMessages.GCMCOMMAND.MOTION_START.name());
			data.put(GcmMessages.ROOM_NAME, this.roomName);
			data.put(GcmMessages.CAMERA_NAME, this.camerName);
			data.put(GcmMessages.PORT, this.port+"");
			data.put(GcmMessages.FILE_PATH, event.getFilePath());
			try {
				GCMManager.sendToDevices(new LinkedList<String>(this.registeredGCMs), data);
			} catch (IOException e) {
				this.handleGCMError(e);
			}
			break;
		}
		case MOTION_END: {
			Map<String, String> data = new TreeMap<String,String>();
			data.put(GcmMessages.COMMAND, GcmMessages.GCMCOMMAND.MOTION_END.name());
			data.put(GcmMessages.ROOM_NAME, this.roomName);
			data.put(GcmMessages.CAMERA_NAME, this.camerName);
			data.put(GcmMessages.PORT, this.port+"");
			data.put(GcmMessages.FILE_PATH, event.getFilePath());
			try {
				GCMManager.sendToDevices(new LinkedList<String>(this.registeredGCMs), data);
			} catch (IOException e) {
				this.handleGCMError(e);
			}
			break;
		}
		
		case STOP:{
			Map<String, String> data = new TreeMap<String,String>();
			data.put(GcmMessages.COMMAND, GcmMessages.GCMCOMMAND.CAMERA_DOWN.name());
			data.put(GcmMessages.ROOM_NAME, this.roomName);
			data.put(GcmMessages.CAMERA_NAME, this.camerName);
			data.put(GcmMessages.PORT, this.port+"");
			try {
				GCMManager.sendToDevices(new LinkedList<String>(this.registeredGCMs), data);
			} catch (IOException e) {
				this.handleGCMError(e);
			}
			break;
		}
		default:
			//nothing to notify the clients about
		}
	}

	private void handleGCMError(IOException e) {
		MotionRecorderEvent event = new MotionRecorderEvent(this, null, MotionRecorderEventType.CONNECTION_ERROR, "GCM connection failed. See attached ioException", "");
		event.setException(e);
		notifyPipelineEvent(event);
		
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
		// requires a installed "motiondetector" plugin in gstreamer

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
		//get the bus
		Bus bus = pipe.getBus();

		//connect error messages na stop the pipe if an error occured
		bus.connect(new Bus.ERROR() {

			@Override
			public void errorMessage(GstObject source, int code, String message) {
				MotionRecorderEvent event = new MotionRecorderEvent(MotionRecorder.this, source, MotionRecorderEventType.GST_ERROR, message, "");
				MotionRecorder.this.notifyPipelineEvent(event);
				MotionRecorder.this.stop();
			}
		});

		//connect info messages
		bus.connect(new Bus.INFO() {

			@Override
			public void infoMessage(GstObject source, int code, String message) {
				MotionRecorderEvent event = new MotionRecorderEvent(MotionRecorder.this, source, MotionRecorderEventType.GST_INFO, message,"");
				MotionRecorder.this.notifyPipelineEvent(event);
			}
		});

		//connect warnings
		bus.connect(new Bus.WARNING() {

			@Override
			public void warningMessage(GstObject source, int code, String message) {
				MotionRecorderEvent event = new MotionRecorderEvent(MotionRecorder.this, source, MotionRecorderEventType.GST_WARNING, message,"");
				MotionRecorder.this.notifyPipelineEvent(event);
			}
		});

		//connect EOS detection and stop the pipe if EOS detected
		bus.connect(new Bus.EOS() {

			private boolean reantrance = false;

			@Override
			public void endOfStream(GstObject source) {

				if (!this.reantrance) {
					this.reantrance=true;
					MotionRecorder.this.stop();
					MotionRecorderEvent event = new MotionRecorderEvent(MotionRecorder.this, source, MotionRecorderEventType.STOP, "EOS detected","");
					MotionRecorder.this.notifyPipelineEvent(event);
				}
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
//		Element enc = ElementFactory.make(FILESAFE_ENCODER, FILESAFE_ENCODER +" on server on port " + this.port);
		Element mux = ElementFactory.make("oggmux", "ogg muxer on server on port " + this.port);
//		Element mux = ElementFactory.make(FILESAFE_MUXER, FILESAFE_MUXER +  " on server on port " + this.port);
		// fileSink
		Element fileSink = ElementFactory.make("filesink", "File Sink on port " + this.port);
		fileSink.set("location", fileName);
		recordBin.addMany(ffmpeg, enc,  mux, fileSink);
		boolean connected = Element.linkMany(ffmpeg, enc,mux,fileSink);
		if (!connected) {
			throw new IllegalStateException("Linking of ffmpeg, enc, mux and fileSink failed on port " + this.port);
		}
//		connected = Element.linkMany(enc, mux);
//		if (!connected) {
//			throw new IllegalStateException("Linking of enc and mux failed on port " + this.port);
//		}
//		connected = Element.linkMany(mux, fileSink);
//		if (!connected) {
//			throw new IllegalStateException("Linking of mux and filesink failed on port " + this.port);
//		}
		// add a ghost pad, so that the bin is accessible from the outside
		Pad staticSourcePad = ffmpeg.getStaticPad("sink");

		GhostPad ghost = new GhostPad("sink", staticSourcePad);
		recordBin.addPad(ghost);

		return recordBin;
	}

	private Bin createPlayBin() {
		Bin playBin = new Bin("playback pipe on port " + this.port);
		Element vidSink = this.createFakeSink();

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

	private Element createFakeSink() {
		return ElementFactory.make("fakesink", "fakesink for streamplayback on port " + this.port);
	}

	private Bin createSourceBin() {
		Bin sourceBin = new Bin("source");
		Element src = ElementFactory.make("tcpserversrc", "tcpserversrc on port "+this.port);
		src.set("port", this.port);
		src.set("host", "0.0.0.0");

		Element demux = ElementFactory.make("oggdemux", "Ogg demuxer on port " + this.port);
		final Element dec = ElementFactory.make("theoradec", "Theora decoder on port " + this.port);
		demux.connect(new Element.PAD_ADDED() {

			@Override
			public void padAdded(Element element, Pad pad) {
				Element.linkMany(element, dec);
				pipe.debugToDotFile(Pipeline.DEBUG_GRAPH_SHOW_NON_DEFAULT_PARAMS, "server_running_playback_on_port_" + MotionRecorder.this.port);
			}
		});

		final Element motionDetection = ElementFactory.make("motiondetector", "motion detection on port " + this.port);
		motionDetection.set("draw_motion", true);
		//add motion detection callbacks
		motionDetection.connect("notify::motion-detected", new Closure() {
			boolean motionStart = true;
			String currentFileName = "";
			@SuppressWarnings("unused") //it is used from the JNA, therefore that it is set as callback
			public void invoke() {
				if (motionStart) {	
					//Assert.isTrue(MotionRecorder.this.isRecording());
					Calendar cal = Calendar.getInstance();
					cal.getTime();
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS");
					String time = sdf.format(cal.getTime());
					String fileName = "Motion_on_port_"+MotionRecorder.this.port+"_"+time + "."+MotionRecorder.FILEENDING;
					String fullFileName = MotionRecorder.this.path + fileName;
					System.out.println("Motion start detected on port " + MotionRecorder.this.port + ". Filename: '" + fullFileName + "'.");
					this.currentFileName = fullFileName;
					MotionRecorder.this.startRec(fullFileName);

					MotionRecorderEvent event = new MotionRecorderEvent(MotionRecorder.this, motionDetection, MotionRecorderEventType.MOTION_START, fullFileName, fullFileName);
					MotionRecorder.this.notifyPipelineEvent(event);

				}else {
					System.out.println("Motion end detected on port " + MotionRecorder.this.port);
					MotionRecorder.this.stopRec(false);
					MotionRecorderEvent event = new MotionRecorderEvent(MotionRecorder.this, motionDetection, MotionRecorderEventType.MOTION_END, this.currentFileName, this.currentFileName);
					MotionRecorder.this.notifyPipelineEvent(event);
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
	private void stopRec(boolean stopPipe) {
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
	private void startRec(String fileName) {
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
			newRecordBin.debugToDotFile(Pipeline.DEBUG_GRAPH_SHOW_NON_DEFAULT_PARAMS, "server_running_recordbin_on_port_" + MotionRecorder.this.port);
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
		//check for reentrance and stop
		if (!this.stopped) {
			// set flag to avoid reentrance and stop the pipe.
			// Stopping the pipe should end in an EOS detected message on the bus, which will inform all listeners
			this.stopped = true;
			this.stopRec(true);
		}
	}

	/**
	 * starts the recorder. This will only play video, but not capturing video
	 */
	public void run() {
		if (this.pipe == null) {
			throw new IllegalStateException("pipe not initialized");
		}

		if (this.stopped) {
			throw new IllegalStateException("pipe only once useable. Build a new pipe");
		}
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
			if (stopPipe) {
				this.pipe.stop();
			}
			return false;
		} else {
			System.out.println("Event received, that is not an eos in '"+pad.getName()+"' on '" + last.getName() + "'  on port " + this.port + ".");
			return true;
		}
	}

	public void setPlayer(VideoComponent vid, final boolean connect, boolean force) {
		final Element vidSink = vid.getElement();
		if (connect) {
			vidSink.setName("SWTVideo on port " + this.port);
		} else {
			//assert vidSink.getName().equals("SWTVideo on port " + this.port);
		}
		// delete the old player (fakesink if connect and vidsink if disconnect) and connect the new player
		List<Element> elementsSorted = this.currentPlayBin.getElementsSorted();
		final Element last = elementsSorted.get(0);
		final Element secondLast = elementsSorted.get(1);
		Pad lastPad = last.getStaticPad("sink");
		int numElements = elementsSorted.size();
		//assert num elements >=2 (minimum queue and playback device)
		Element first = elementsSorted.get(numElements-1);

		//check if its playing. If not the elements can be changed without problems. And on the other hand does the blocking not return when no dataflow is handled.
		if (!this.pipe.isPlaying() || force) {
			// not running, means stopped or not started yet. Emulate EOS received
			if (connect) {
				this.handleEOSOnPlayBin(vidSink, last, secondLast, lastPad, null, new EOSEvent(), null);
			} else {
				// assert last == vidSink
				Element newFakeSink = MotionRecorder.this.createFakeSink();
				this.handleEOSOnPlayBin(newFakeSink, last, secondLast, lastPad, null, new EOSEvent(), null);
			}
		} else {
			// pipe is running, change it dynamically
			//get the first src pad in the pipeline to block it
			final Pad firstPad = first.getStaticPad("src");
			//block the outgoing pad of the first element (should be queue)
			lastPad.addEventProbe(new Pad.EVENT_PROBE() {


				@Override
				public boolean eventReceived(Pad pad, Event event) {
					if (connect) {
						return MotionRecorder.this.handleEOSOnPlayBin(vidSink, last, secondLast, pad, firstPad, event, this);
					} else {
						// assert last == vidSink
						Element newFakeSink = MotionRecorder.this.createFakeSink();
						return MotionRecorder.this.handleEOSOnPlayBin(newFakeSink, last, secondLast, pad, firstPad, event, this);
					}
				}
			});
			firstPad.setBlocked(true);


			//get the next element and send an eos event on it (should normally be the playback device (fakesink or video)
			Element second = elementsSorted.get(numElements-2);
			Pad sinkPad = second.getStaticPad("sink");
			sinkPad.sendEvent(new EOSEvent());
		}
	}

	protected boolean handleEOSOnPlayBin(Element newSink, Element last, Element secondLast, Pad occuredPad, Pad blockedPad, Event event, EVENT_PROBE probe) {
		if (event instanceof EOSEvent) {
			System.out.println("EOS received in '"+occuredPad.getName() +"' on '" + last.getName() + "'.");
			boolean removed = this.currentPlayBin.remove(last);
			if (!removed) {
				throw new IllegalStateException("current play bin cannot be removed from the pipe on port " + this.port);
			}
			last.stop();
			this.currentPlayBin.addMany(newSink);
			boolean connected = Element.linkMany(secondLast, newSink);
			if (!connected) {
				throw new IllegalStateException("can not link the new vidSink on port " + this.port);
			}
			newSink.syncStateWithParent();
			//remove the blocking status if something is blocked
			if (blockedPad != null) {
				//blocked => unblock
				blockedPad.setBlocked(false);
			}

			//remove the probe if there is one connected
			if (probe != null) {
				// connecnted probe => disconnect it
				occuredPad.removeEventProbe(probe);
			}
			return false;
		} else {
			System.out.println("Event received, that is not an eos in '"+occuredPad.getName()+"' on '"+ last.getName() + "'.");
			return true;
		}
	}

	public String getName() {
		String name = this.camerName + " in " + this.roomName + " (port " + this.port+ ")";
		return name;
	}

	public String getRoomName() {
		return this.roomName;
	}

	public String getCameraName() {
		return this.camerName;
	}

	public boolean isGCMRegistered(String gcm) {
		// returns if the gcm is registered for this recorder
		return this.registeredGCMs.contains(gcm);
	}

	public void deregisterGCM(String gcm) {
		// delete this gcm from the registered clients
		this.registeredGCMs.remove(gcm);

	}

	public int getPort() {
		return this.port;
	}

	public void registerGCM(String gcm) {
		this.registeredGCMs.add(gcm);
	}

	public boolean getSubscriptionStatus(String regId) {
		return this.registeredGCMs.contains(regId);
	}

}
