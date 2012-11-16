package GUI;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Gst;
import org.gstreamer.Pipeline;
import org.gstreamer.StateChangeReturn;
import org.gstreamer.event.EOSEvent;
import org.gstreamer.swt.VideoComponent;


public class Gui {

	static private Pipeline pipe;
	static private Element record_queue;
	static private Element switcher;
	static private Element enc;
	static private Element mux;
	static private Element fileSink;
	

	public static void main(String[] args){
		args = Gst.init("SWTMultimediaVideo", args);
		Display display = new Display();
		Shell shell = new Shell(display);
		configureGUI(shell);
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		pipe.stop();
		display.dispose();
	}

	private static void configureGUI(final Shell shell) {
		//set the titel
		shell.setText("Multimeda System 2012 Luleå - Lab 1");
		// add fullscreen button
		final Button button_fullScreen = new Button (shell, SWT.PUSH);
		button_fullScreen.setText ("Fullscreen...");
		//add fullscreen switch
		button_fullScreen.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				shell.setFullScreen(!shell.getFullScreen());
			}
		});

		final Button button_stop = new Button (shell, SWT.PUSH);
		button_stop.setText ("Stop!");
		button_stop.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pipe.stop();
			}
		});

		final Button button_play = new Button (shell, SWT.PUSH);
		button_play.setText ("Play");
		button_play.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pipe.play();
			}
		});
		
		final Button button_record = new Button (shell, SWT.PUSH);
		button_record.setText ("Record");
		
		final Element tee = ElementFactory.make("tee", "Tee split buffer");
		

		pipe = new Pipeline("Gstreamer play/record pipe");
		Element src = ElementFactory.make("v4l2src", "video capturing source");

		
		
		//add video windows
		VideoComponent vid = new VideoComponent(shell, SWT.NONE);
		vid.showFPS(true);
		Element sink = vid.getElement();
		sink.setName("SWTVideo");

		Element play_queue= ElementFactory.make ("queue", "playback queue");
		play_queue.set("leaky", 1);

		//Element motionDetect = ElementFactory.make("motioncells", "motion detector of OpenCV");

		vid.setKeepAspect(true);
		vid.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		record_queue= ElementFactory.make ("queue", "recording queue");
		switcher = ElementFactory.make("valve", "Switcher for recording");
		switcher.set("drop", true);

		pipe.addMany(src, tee, play_queue, sink, record_queue, switcher);
		boolean b1 = Element.linkMany(src, tee);
		if (!b1) {
			//TODO
			throw new IllegalStateException("b1");
		}
		boolean b2 = Element.linkMany(tee, record_queue, switcher);
		if (!b2) {
			//TODO
			throw new IllegalStateException("b2");
		}
		boolean b3 = Element.linkMany(tee, play_queue,sink);
		if (!b3) {
			//TODO
			throw new IllegalStateException("b3");
		}
		
		
		
		button_record.addSelectionListener(new SelectionAdapter() {
			int counter = 0;
			public void widgetSelected(SelectionEvent e) {
				if (counter == 0) {
					setupRecording();
					counter=1;
				}
				
				Boolean notRecording = (Boolean)switcher.get("drop");
				
				if (!notRecording) {
					//recording => stop recording
					switcher.set("drop", !notRecording);
					enc.sendEvent(new EOSEvent());
					enc.stop();
					mux.stop();
					fileSink.stop();
					fileSink.set("location", "/home/marc/test"+(counter++)+".avi");
					button_record.setText ("Record");
				} else {
					//not recording => recording
					fileSink.play();
					mux.play();
					enc.play();
					//State state = fileSink.getState();
					switcher.set("drop", !notRecording);
					button_record.setText("Stop Rec");
				}
			}
		});
        pipe.play();
		
		
		shell.setDefaultButton (button_fullScreen);
		GridLayout layout = new GridLayout(3, false);
		shell.setLayout(layout);
		//shell.setMinimumSize(300, 100);
		shell.pack ();
	}
	
	private static void setupRecording(){
		//TODO preference windows
		//enc = ElementFactory.make("x264enc", "avi Encoder");
		enc = ElementFactory.make("theoraenc", "Encoder ogg");
		//mux = ElementFactory.make("avimux", "avi Muxer");
		mux = ElementFactory.make("oggmux", "Ogg Muxer");
		//TODO add file dialog
		fileSink = ElementFactory.make("filesink", "File Sink");
		fileSink.set("location", "/home/marc/test0.avi");
		pipe.addMany(enc, mux, fileSink);
			
		boolean b4 = Element.linkMany(switcher, enc, mux, fileSink);
		if (!b4) {
			//TODO
			throw new IllegalStateException("b4");
		}
		
		StateChangeReturn p1 = enc.play();
		StateChangeReturn p2 = mux.play();
		StateChangeReturn p3 = fileSink.play();
	}
}
