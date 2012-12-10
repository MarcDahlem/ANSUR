/**
 * 
 * @author marc
 * This is the GUI class used in the first Lab of the Multimedia Systems lecture 2012 in Lulea.
 * It is able to control a recorder, that can record and play simultaniously from a connected webcam.
 * The GUI consist of 3 different modes.
 * MODE1 DEFAULT: It has the default mode, without fullscreen and a shown menu all the time.
 * MODE2 FULLSCREEN WITHOUT MENU: A fullscreen that shows only the video part. If one moves the mouse towards the boarders, it will switch to MODE3.
 * MODE3 FULLSCREEN WITH MENU: A fullscreen that shows the video part for the webcam, and a menu to control the recorder.
 * 		 It is shown menu_off_time ms (default 3000) after movement of the curser stopped. Then it will switch back to MODE2. 
 */

package gui;

import java.io.IOException;
import java.net.UnknownHostException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.gstreamer.GstObject;

import connectionPipe.ConnectionPipe;
import connectionPipe.ConnectionPipeEvent;
import connectionPipe.ConnectionPipeEventType;
import connectionPipe.ConnectionPipeListener;
import data.RecorderHost;


public class Gui {

	private Image bgImmage; //the current background image, computed on every rezise of the window
	private ConnectionPipe currentPipe;
	private Display display;
	private ConnectionPipeListener pipeListener;
	private Button button_stop;
	private Button button_play;
	private Button button_settings;
	protected RecorderHost host;

	public Gui() {
		//set the default server host values
		this.host=new RecorderHost("localhost", 5000);

		//create a listener for the pipelines
		this.pipeListener = new ConnectionPipeListener() {

			@Override
			public void eventAppeared(ConnectionPipeEvent event) {
				//check which pipeline event appeared and handle it
				final ConnectionPipeEventType eventType = event.getEventType();
				final String message = event.getMessage();
				final GstObject gstSource = event.getGstSource();


				switch (eventType) {
				case STOP:
					Gui.this.display.asyncExec(new Runnable() {

						@Override
						public void run() {
							Gui.this.disconnect();
							MessageBox msgBox = new MessageBox(Gui.this.display.getShells()[0], SWT.OK);
							msgBox.setMessage("EOS received from '" + gstSource.getName() + "'. Pipe stopped.");
							msgBox.setText("Information");
							msgBox.open();
						}
					});
					break;
				case GST_ERROR:
					Gui.this.display.asyncExec(new Runnable() {

						@Override
						public void run() {
							Gui.this.disconnect();
							MessageBox msgBox = new MessageBox(Gui.this.display.getShells()[0], SWT.OK|SWT.ERROR);
							msgBox.setMessage("Error on '" + gstSource.getName()+ "': "+ message);
							msgBox.setText("Error");
							msgBox.open();
						}
					});
					break;
				case GST_INFO:
					Gui.this.display.asyncExec(new Runnable() {

						@Override
						public void run() {
							MessageBox msgBox = new MessageBox(Gui.this.display.getShells()[0], SWT.OK);
							msgBox.setMessage("Info from '" + gstSource.getName() + "': " + message);
							msgBox.setText("Information");
							msgBox.open();
						}
					});
					break;
				case GST_WARNING:
					Gui.this.display.asyncExec(new Runnable() {

						@Override
						public void run() {
							MessageBox msgBox = new MessageBox(Gui.this.display.getShells()[0], SWT.OK| SWT.ERROR);
							msgBox.setMessage("Warning from '" + gstSource.getName() + "': " + message+ "\n It CAN influence the work behavior and it could lead to problems during the execution...");
							msgBox.setText("Warning");
							msgBox.open();
						}
					});
					break;
				default:
					Gui.this.display.asyncExec(new Runnable() {

						@Override
						public void run() {
							MessageBox msgBox = new MessageBox(Gui.this.display.getShells()[0], SWT.OK);
							msgBox.setMessage("Unknown event appeared: '"+eventType.name() + "' on '" + gstSource.getName() + "': " +message);
							msgBox.setText("Information");
							msgBox.open();
						}
					});
				}
			}
		};
	}

	/**
	 * This method creates the GUI with StackLayouts for all 3 modes.
	 * 
	 * @param shell the shell used as parent for the whole application
	 */
	private void configureGUI(Shell shell) {
		//set the titel
		shell.setText("Multimedia Systems 2012 Luleå - Lab 2 Client");

		//configure layout of the shell
		shell.setLayout(new GridLayout(3, false));
		GridData gridData = new GridData(GridData.FILL_BOTH);
		shell.setLayoutData(gridData);
		shell.setMinimumSize(250, 100);
		shell.setSize(250,100);

		//set a nice background
		this.setBackground(shell);

		//add the buttons
		this.addButtons(shell);

	}


	/**
	 * This method makes the gui a lot nicer and sets a nice background to all composites used in this gui
	 * 
	 * @param shell the shell where the background should be set
	 */
	private void setBackground(final Shell shell) {
		//first set the inherit mode, so that the background is inherited from all subparts of the shell
		shell.setBackgroundMode(SWT.INHERIT_DEFAULT);

		//add a listener to recompute the background if the window is resized
		shell.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event event) {
				Gui.this.refreshBackground(shell);
			}
		});
	}

	/**This method recalculates the background. Normally used when a resze event occured
	 * 
	 * @param shell the shell used to refresh the background on
	 */
	private void refreshBackground(Shell shell) {
		//get the new size and create the new image
		Rectangle rect = shell.getClientArea();
		Image newImage = new Image(this.display, 1, Math.max(1, rect.height));

		//create a new flowding image
		GC gc = new GC(newImage);

		//TODO preferences for the color picker
		//create a new image with flowing from color 1 to color 2 vertically with the sizes given in the new size of the shell.
		gc.setForeground(this.display.getSystemColor(SWT.COLOR_DARK_CYAN));
		gc.setBackground(this.display.getSystemColor(SWT.COLOR_DARK_GRAY));
		gc.fillGradientRectangle(rect.x, rect.y, 1, rect.height, true);
		// like shown in the API has this image to be disposed after creation
		gc.dispose();

		//set the new background image
		shell.setBackgroundImage(newImage);

		// dispose the old image if existent and set it to the new image
		if (bgImmage != null) {
			bgImmage.dispose();
		}
		bgImmage = newImage;
	}

	/**
	 * Adds the menu to the given composite with centralized buttons to control the recorder.
	 * side effect: the composite {@link #menuBarDefaultComp} is set so that the menubar can easily be connected/disconnected to this composit
	 * @param parent the parent composite where the menu-buttons should be added
	 */
	private void addButtons(Composite parent) {
		// create the MenuBar

		//the gridlayout is used to center the buttons. Add hidden fields left and right to center...
		addHiddenField(parent);

		//create a new composite for the middle, to create boarder around all the buttons
		Composite middle = new Composite(parent, SWT.BORDER);
		middle.setLayout(new GridLayout(3, false));
		middle.setLayoutData(new GridData(GridData.FILL_BOTH));


		//add stop button
		final Button button_stop = new Button (middle, SWT.PUSH);
		button_stop.setLayoutData(new GridData(GridData.FILL_BOTH));
		button_stop.setText ("Stop!");

		//add play button and connect it to its action
		final Button button_play = new Button (middle, SWT.PUSH);
		button_play.setLayoutData(new GridData(GridData.FILL_BOTH));
		button_play.setText ("Connect");

		//add a settings button
		final Button button_settings = new Button (middle, SWT.PUSH);
		button_settings.setLayoutData(new GridData(GridData.FILL_BOTH));
		button_settings.setText ("Settings");

		//disable stop button at the beginning
		button_stop.setEnabled(false);

		//add the actions for the buttons

		//add action to the connect button
		button_play.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				//if the connection button is clicked, a pipe with video should be created to connect with a server
				Gui.this.connect();
			}
		});

		// add the action to the stop button
		button_stop.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Gui.this.disconnect();
			}
		});

		//add action to the settings button
		button_settings.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//open a dialog for set port and host
				Shell shell = Gui.this.display.getActiveShell();
				ClientSettingsDialog settingsDialog = new ClientSettingsDialog(shell, Gui.this.host);
				RecorderHost recorderHost = settingsDialog.open();
				if (recorderHost != null) {
					//not cancelled
					Gui.this.host = recorderHost;
				}
			}
		});
		//add another empty layout at the right side so that the buttons are centered. It has to grab horizontal size
		addHiddenField(parent);

		//set the buttons
		this.button_stop = button_stop;
		this.button_play = button_play;
		this.button_settings = button_settings;
	}

	/**
	 *  creates a hidden field on the given Composite. This field is to be used in a GridLayout and grabs horizontal space
	 * @param comp Composite, where the hidden field should be created at
	 */
	private void addHiddenField(Composite comp) {
		Label label = new Label(comp, SWT.NONE);
		label.setVisible(false);
		//it has to be excessing the horizontal space. Without a right side that would leed to buttons always on the right sode of the menu bar
		GridData data = new GridData(SWT.FILL, SWT.NONE, true, false); 
		label.setLayoutData(data);
	}

	/**
	 * Start a new connection pipeline.
	 */

	private void connect() {
		this.setButtonsConnected();
		//in order to connect create a new pipe, initalize it and run it
		ConnectionPipe pipe;
		try {
			pipe = new ConnectionPipe(this.host.getHostName(), this.host.getRemotePipelinePort());
			pipe.addConnectionPipeListener(this.pipeListener);
			pipe.init();
			pipe.run();
			this.currentPipe = pipe;
		} catch (UnknownHostException e) {
			this.handleConnectionError(e);
		} catch (IOException e) {
			this.handleConnectionError(e);
		}
	}

	private void handleConnectionError(final Exception e) {
		// show message window and disconnect the client
		Gui.this.display.asyncExec(new Runnable() {

			@Override
			public void run() {
				MessageBox msgBox = new MessageBox(Gui.this.display.getShells()[0], SWT.OK|SWT.ERROR);
				msgBox.setMessage("Error whith the tcp socket:'" + e.getMessage()+ "'.");
				msgBox.setText("Error");
				msgBox.open();
			}
		});
		//stop the clients connection
		Gui.this.disconnect();
	}

	private void setButtonsDisconnected() {
		if (!this.button_play.isDisposed()) {
			this.button_play.setEnabled(true);
			//set button color
			this.button_play.setBackground(Gui.this.display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		}

		if (!this.button_stop.isDisposed()) {
			this.button_stop.setEnabled(false);
		}		

		if (!this.button_settings.isDisposed()) {
			this.button_settings.setEnabled(true);
		}

	}

	private void setButtonsConnected() {
		//set button enablements
		this.button_play.setEnabled(false);
		this.button_stop.setEnabled(true);
		this.button_settings.setEnabled(false);

		//set the connect background color
		this.button_play.setBackground(Gui.this.display.getSystemColor(SWT.COLOR_GREEN));
	}

	/**
	 * stops the connection
	 */
	private void disconnect() {
		this.setButtonsDisconnected();
		if (this.currentPipe != null) {
			this.currentPipe.stop();
			this.currentPipe.removeConnectionPipeListener(this.pipeListener);
			this.currentPipe = null;
		}
	}

	/**
	 * Runs the gui, until it is closed by a user. This Method will first return, when the window is disposed
	 * Will fail, if the GUI was not initialized by using the method {@link #init()}.
	 */
	public void run() {

		//get the shell and open it
		Shell shell= this.display.getShells()[0];
		shell.open();

		//run as long as it isnt disposed
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}

		//disposed command received: disconnect the pipe, but before disconnect the listener
		this.disconnect();
		if (bgImmage != null) {
			bgImmage.dispose();
		}
		this.display.dispose();
	}

	/**
	 * Initializes all the GUI parts and has to be called before using the {@link #run()} method.
	 * Will also set the {@link #display} variable in this gui.
	 */
	public void init() {
		//create the display and shell
		Display display = new Display();
		Shell shell = new Shell(display);
		//create all the subparts of the shell
		this.configureGUI(shell);

		//finally set display variable
		this.display = display;
	}
}
