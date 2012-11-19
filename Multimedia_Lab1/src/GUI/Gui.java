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
import org.gstreamer.swt.VideoComponent;


public class Gui {
	private static final String CapturingPaths = "/home/marc/";
	private static final String CapturingName = "testcambin";
	private static final String CapturingEnding = ".avi";
	
	private Display display;
	private Shell shell;
	private VideoComponent vid;
	private boolean fps;
	private Recorder recorder;
	private int recorderCounter = 0;
	private Button button_fullScreen;
	private Button button_stop;
	private Button button_play;
	private Button button_record;

	private void configureGUI() {
		//set the titel
		this.shell.setText("Multimeda System 2012 Luleå - Lab 1");

		//set the layout to gridlayout
		Layout layout = new GridLayout(4, false);
		//layout.type=SWT.VERTICAL;
		this.shell.setLayout(layout);
		this.shell.setMinimumSize(600, 400);



		//add video windows
		//final Composite composite = new Composite(shell, SWT.CENTER);
		//composite.setLayout(new FillLayout());
		//Composite composite2 = new Composite(shell, SWT.CENTER);
		this.vid = new VideoComponent(shell, SWT.NONE);

		GridData data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan=4;
		data.grabExcessVerticalSpace=true;
		data.grabExcessHorizontalSpace=true;
		this.vid.setLayoutData(data);
		this.vid.setKeepAspect(true);
		//vid.setLayoutData(new GridData(GridData.FILL_BOTH));

		// add fullscreen button
		this.button_fullScreen = new Button (shell, SWT.PUSH);
		this.button_fullScreen.setText ("Fullscreen...");

		this.button_stop = new Button (shell, SWT.PUSH);
		this.button_stop.setText ("Stop!");
		this.button_stop.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Gui.this.StopRecorder();
			}
		});

		this.button_play = new Button (shell, SWT.PUSH);
		this.button_play.setText ("Play");
		this.button_play.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Gui.this.playRecorder();
			}
		});

		this.button_record = new Button (shell, SWT.PUSH);
		this.button_record.setText ("Record");

		//add fullscreen switch
		this.button_fullScreen.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Gui.this.setFullScreen();
			}
		});

		this.button_record.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean recording = Gui.this.recorder.isRecording();
				if (recording) {
					Gui.this.stopRec();
				} else {
					//not recording => recording
					Gui.this.startRec();
				}
			}
		});
		shell.setDefaultButton (this.button_play);
		
		shell.pack ();
	}

	private void setFullScreen() {
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

	private void startRec() {
		String fileName= CapturingPaths+CapturingName+(this.recorderCounter++)+CapturingEnding;
		this.recorder.startRec(fileName);
		button_record.setText("Stop rec");
	}

	private void stopRec() {
		this.recorder.stopRec();
		button_record.setText("Record");
	}

	private void playRecorder() {
		this.recorder.play();
	}

	private void StopRecorder() {
		this.recorder.stop();
		
	}

	public void run() {
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		this.recorder.stop();
		this.display.dispose();
	}

	public void init() {
		this.display = new Display();
		this.shell = new Shell(display);
		this.configureGUI();
	}
	
	private void changeShowFPS() {
		this.fps =!fps;
		this.vid.showFPS(this.fps);
	}

	public VideoComponent getVideoComponent() {
		return this.vid;
	}

	public void setRecorder(Recorder rec) {
		if (rec != null) {
		  this.recorder=rec;
		} else {
			throw new IllegalArgumentException("Recorder cannot be null");
		}
	}
}
