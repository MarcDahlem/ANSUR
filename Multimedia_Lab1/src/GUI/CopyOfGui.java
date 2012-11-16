package GUI;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Gst;
import org.gstreamer.swt.VideoComponent;


public class CopyOfGui {

	private static final String CapturingPaths = "/home/marc/";
	private static final String CapturingName = "testcambin";
	private static final String CapturingEnding = ".avi";
	static private Element pipe;

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

		//set the layout to gridlayout
		Layout layout = new GridLayout(4, false);
		//layout.type=SWT.VERTICAL;
		shell.setLayout(layout);
		shell.setMinimumSize(600, 400);



		//add video windows
		//final Composite composite = new Composite(shell, SWT.CENTER);
		//composite.setLayout(new FillLayout());
		//Composite composite2 = new Composite(shell, SWT.CENTER);
		final VideoComponent vid = new VideoComponent(shell, SWT.NONE);
		vid.showFPS(true);

		GridData data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan=4;
		data.grabExcessVerticalSpace=true;
		data.grabExcessHorizontalSpace=true;
		vid.setLayoutData(data);
		vid.setKeepAspect(true);
		//vid.setLayoutData(new GridData(GridData.FILL_BOTH));

		// add fullscreen button
		final Button button_fullScreen = new Button (shell, SWT.PUSH);
		button_fullScreen.setText ("Fullscreen...");

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

		pipe = ElementFactory.make("camerabin", "cambin");

		Element src = ElementFactory.make("v4l2src", "video capturing source");
		pipe.set("video-source", src);




		Element sink = vid.getElement();
		sink.setName("SWTVideo");

		//TODO preference windows
		//Element enc = ElementFactory.make("x264enc", "avi Encoder");
		Element enc = ElementFactory.make("theoraenc", "Encoder ogg");
		//Element mux = ElementFactory.make("avimux", "avi Muxer");
		Element mux = ElementFactory.make("oggmux", "Ogg Muxer");

		pipe.set("viewfinder-sink", sink);
		pipe.set("video-encoder", enc);
		pipe.set("video-muxer", mux);
		pipe.set("mode", 1);
		pipe.set("mute", true);

		//add fullscreen switch
		button_fullScreen.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				button_play.dispose();
				button_record.dispose();
				button_fullScreen.dispose();
				button_stop.dispose();
				vid.getShell().setFullScreen(!shell.getFullScreen());
				Menu menu = new Menu(vid);
				MenuItem item = new MenuItem (menu, SWT.NONE);
				item.setText("test");
				vid.setMenu(menu);
			}
		});

		button_record.addSelectionListener(new SelectionAdapter() {
			int counter = 0;
			boolean recording = false;
			public void widgetSelected(SelectionEvent e) {

				if (recording) {
					//recording => stop recording
					pipe.emit("capture-stop");
					button_record.setText("Record");
				} else {
					//not recording => recording
					String fileName= CapturingPaths+CapturingName+(counter++)+CapturingEnding;
					pipe.set("filename", fileName);
					pipe.emit("capture-start");
					button_record.setText("Stop rec");
				}

				recording = !recording;
			}
		});
		pipe.play();


		shell.setDefaultButton (button_fullScreen);
		shell.pack ();
	}
}
