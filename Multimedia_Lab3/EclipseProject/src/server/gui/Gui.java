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

package server.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeSet;


import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.gstreamer.GstObject;
import org.gstreamer.swt.VideoComponent;

import commonUtility.ConnectionEventType;

import server.connectionManager.ConnectionEvent;
import server.connectionManager.ConnectionListener;
import server.connectionManager.ConnectionManager;
import server.motionRecorder.MotionRecorder;
import server.motionRecorder.MotionRecorderEvent;
import server.motionRecorder.MotionRecorderEventType;
import server.motionRecorder.MotionRecorderListener;



public class Gui {

	private static final int SERVER_PORT = 5000;

	private static final int OVERLAY_TIME = 5000; //milliseconds

	//These Strings are used to build the filename, where the recorded files are saved.
	private static String CapturingPath = null;

	private int menu_off_time = 3000; // time until the menu in fullscreen mode turns off in ms

	private Display display; //display of the gui
	private volatile VideoComponent vid; //the videocomponent, that has to be shifted around when changing to fullscreen or changing menu on/off
	private MotionRecorder current_recorder;  //the gstreamer recorder, that is to be controlled with this gui

	private KeyListener listener_keypress; //listener that is used to connect keypresses to actions 
	private Image bgImmage; //the current background image, computed on every rezise of the window
	private Composite fsComp;  // the composite for fullscreen
	private Composite defaultVidComp; //the composite if no fullscreen is used
	private MouseMoveListener listener_mouseMove_woMen; //listener for the fullscreenwindow without a shown menu
	private MouseMoveListener listener_mouseMove_wMen; //listener for fullscreen with menu
	private Runnable menu_timer;  //the timer used to switch the menu off

	private Composite menuBarDefaultComp;
	private Composite menuBarFSComp;
	private MotionRecorderListener listener_pipe;
	private Button button_stop;
	private Button button_play;
	private Button button_record;
	private ConnectionListener listener_connectionManager;
	private ConnectionManager currentConnectionManager;
	private volatile ArrayList<MotionRecorder> pipeList;

	private volatile String currentOverlayMessage;

	private TreeSet<String> gcmSet; //list with all connected google cloud message ids

	/**
	 * The constructur of the Gui initialises counters, listeners and the timer.
	 * The GUI is not configured after the constructor finishes.
	 * Use the method {@link #init()} to initialize the gui.
	 * And {@link #run()} after that to run the gui, until it will be disposed.
	 */
	public Gui() {
		//create the list containing all recorders for the connect streams
		this.pipeList = new ArrayList<MotionRecorder>();

		//create the list that contains all google cloud message ids from the connected clients
		this.gcmSet = new TreeSet<String>();

		//initialize the default overlay text
		this.currentOverlayMessage="";

		//create the listener to react on key presses
		this.listener_keypress = new KeyListener() {

			@Override
			public void keyReleased(KeyEvent e) {
				//nothing to do
			}

			@Override
			public void keyPressed(KeyEvent e) {
				switch (e.character) {
				case 'f':
					//f pressed, switch fullscreen mode on/off
					Gui.this.changeFullScreen();
					break;
				default:
					//do nothing, unimplmeneted key pressed
				}
			}
		};

		//create listener for the fullscreen, when no menu is opened
		this.listener_mouseMove_woMen = new MouseMoveListener() {

			@Override
			public void mouseMove(MouseEvent e) {
				//if mouse is moved, change to fullscreen mode with menu
				Gui.this.changeFSMenu();
				// in order to check movements then also on the video-part, add the listener for fullscreen with menu to this part too
				Gui.this.vid.addMouseMoveListener(Gui.this.listener_mouseMove_wMen);

				// start the menu timer
				e.display.timerExec(Gui.this.menu_off_time, Gui.this.menu_timer);
			}

		};


		//create listener for the fullscreen mode with menu. It should update the timer everytime a movement is detected (on video part and on other parts)
		this.listener_mouseMove_wMen = new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent e) {
				//movement detected, update the time for our timer
				e.display.timerExec(Gui.this.menu_off_time, Gui.this.menu_timer);
			}

		};

		//finally we have to define the timer for the listeners above. It is a runnable that will be executed from the display after the time is expired
		this.menu_timer=new Runnable (){
			@Override
			public void run() {
				//turn off fullscreenmode
				Gui.this.changeFSMenu();
				//remove the withmenu listener from the video part. The video part is the only part visible in the new mode "fullscreen without menu"
				Gui.this.vid.removeMouseMoveListener(Gui.this.listener_mouseMove_wMen);
			}

		};

		//create a listener for the pipelines
		this.listener_pipe = new MotionRecorderListener() {

			@Override
			public void eventAppeared(MotionRecorderEvent event) {
				final MotionRecorderEventType eventType = event.getEventType();
				final String message = event.getMessage();
				final GstObject gstSource = event.getGstSource();
				final MotionRecorder source = (MotionRecorder)event.getSource();


				switch (eventType) {
				case STOP:
					Gui.this.display.asyncExec(new Runnable() {

						@Override
						public void run() {
							Gui.this.stopRecorder(source);
							MessageBox msgBox = new MessageBox(Gui.this.display.getShells()[0], SWT.OK|SWT.ICON_WORKING);
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
							Gui.this.stopRecorder(source);
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
							MessageBox msgBox = new MessageBox(Gui.this.display.getShells()[0], SWT.OK | SWT.ICON_INFORMATION);
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
							MessageBox msgBox = new MessageBox(Gui.this.display.getShells()[0], SWT.OK| SWT.ICON_WARNING);
							msgBox.setMessage("Warning from '" + gstSource.getName() + "': " + message+ "\n It CAN influence the work behavior and it could lead to problems during the execution...");
							msgBox.setText("Warning");
							msgBox.open();
						}
					});
					break;
				case MOTION_START:
					Gui.this.display.asyncExec(new Runnable() {

						@Override
						public void run() {
							//switch video sink to the latest motion detection stream
							if (source != Gui.this.current_recorder) {
								if (Gui.this.current_recorder != null) {
									Gui.this.current_recorder.setPlayer(Gui.this.vid, false, false);
								}
								//set the new video sink
								Gui.this.current_recorder = source;
								source.setPlayer(Gui.this.vid, true, false);
							}
							// update the overlay
							Gui.this.updateOverlay("Motion recording started on '" + source.getName()+"'.");

						}
					});
					break;
				case MOTION_END:
					//TODO filename in list for check on download if it is still recording.
					Gui.this.display.asyncExec(new Runnable() {

						@Override
						public void run() {
							// update the overlay
							Gui.this.updateOverlay("Motion recording stopped on '" + source.getName()+"'.");
						}
					});
					break;
				default:
					Gui.this.display.asyncExec(new Runnable() {

						@Override
						public void run() {
							MessageBox msgBox = new MessageBox(Gui.this.display.getShells()[0], SWT.OK| SWT.ICON_INFORMATION);
							msgBox.setMessage("Unknown event appeared: '"+eventType.name() + "' on '" + gstSource.getName() + "': " +message);
							msgBox.setText("Information");
							msgBox.open();
						}
					});
				}
			}
		};

		this.listener_connectionManager = new ConnectionListener() {

			@Override
			public void eventAppeared(ConnectionEvent event) {
				final ConnectionEventType eventType = event.getEventType();
				switch (eventType) {
				case CAM_CONNECT_GET_PORT:
					int eventPort = event.getPort();
					String roomName = event.getRoomName();
					String cameraName = event.getCameraName();
					// Assert.isTrue(Gui.CapturingPath != null);
					MotionRecorder recorder = new MotionRecorder(eventPort, roomName, cameraName, Gui.CapturingPath);
					recorder.addMotionRecorderListener(Gui.this.listener_pipe);
					recorder.init();
					//connect video if its the first stream
					if (Gui.this.current_recorder == null) {
						recorder.setPlayer(Gui.this.vid, true, false);
						Gui.this.current_recorder = recorder;
					}
					Gui.this.pipeList.add(recorder);
					recorder.run();
					Gui.this.updateOverlay("Client connected on port " + eventPort);
					break;
				case CLIENT_REGISTER: {
					String gcm = event.getGCM();
					//client tries to register. Add it to the gcm list if not in
					if (Gui.this.gcmSet.contains(gcm)) {
						event.errorAppeared();
					} else {
						Gui.this.gcmSet.add(gcm);
					}
					break;
				}
				case CLIENT_DEREGISTER: {
					String gcm = event.getGCM();
					//client tries to deregister. Delete it from the gcm list if existent and remove it from all movie notifications
					if (Gui.this.gcmSet.contains(gcm)) {
						Gui.this.deleteAllNotifications(gcm);
						Gui.this.gcmSet.remove(gcm);
					} else {
						event.errorAppeared();
					}
					break;
				}
				case CLIENT_SUBSCRIBE: {
					String gcm = event.getGCM();
					int port = event.getPort();


					boolean success = Gui.this.subscribe(gcm,port);

					if (!success) {
						event.errorAppeared();
					}

					break;
				}
				case CLIENT_UNSUBSCRIBE: {
					String gcm = event.getGCM();
					int port = event.getPort();


					boolean success = Gui.this.unsubscribe(gcm,port);

					if (!success) {
						event.errorAppeared();
					}

					break;
				}
				case CLIENT_GET_CAMS: {
					// add all recorders to the event
					for (MotionRecorder rec:Gui.this.pipeList) {
						event.addMotionRecorder(rec);
					}
				}
				default:
					Gui.this.display.asyncExec(new Runnable() {

						@Override
						public void run() {
							MessageBox msgBox = new MessageBox(Gui.this.display.getShells()[0], SWT.OK| SWT.ICON_INFORMATION);
							msgBox.setMessage("Unknown event appeared: '"+eventType.name() + "'.");
							msgBox.setText("Information");
							msgBox.open();
						}
					});
				}
			}
		};
	}

	protected boolean unsubscribe(String gcm, int port) {
		// unsubscribe the gcm from recorder on the given port
		if (!Gui.this.gcmSet.contains(gcm)) {
			//not a valid gcm id
			return false;
		}

		for (MotionRecorder recorder: this.pipeList) {
			if (recorder.getPort() == port) {
				recorder.deregisterGCM(gcm);
				return true;
			}
		}

		// no recorder on the given port found
		return false;
	}

	private boolean subscribe(String gcm, int port) {
		// subscribe the gcm to the given port
		if (!Gui.this.gcmSet.contains(gcm)) {
			//not a valid gcm id
			return false;
		}

		for (MotionRecorder recorder: this.pipeList) {
			if (recorder.getPort() == port) {
				recorder.registerGCM(gcm);
				return true;
			}
		}

		// no recorder on the given port found
		return false;

	}

	protected void deleteAllNotifications(String gcm) {
		// delete the given gcm from all motion recorders
		for (MotionRecorder recorder:this.pipeList) {
			if (recorder.isGCMRegistered(gcm)) {
				recorder.deregisterGCM(gcm);
			}
		}

	}

	private synchronized void updateOverlay(final String overlayMessageToAdd) {
		this.display.asyncExec(new Runnable() {

			@Override
			public void run() {
				if (overlayMessageToAdd == null) {
					throw new IllegalArgumentException();
				}
				String newOverlayMessage;

				//append the new message to the current one. If there is one, append it in a new line.
				if (Gui.this.currentOverlayMessage.isEmpty()) {
					newOverlayMessage = overlayMessageToAdd;
				} else {
					newOverlayMessage = Gui.this.currentOverlayMessage+System.getProperty("line.separator") + overlayMessageToAdd;
				}

				Gui.this.vid.setOverlay(newOverlayMessage);
				Gui.this.currentOverlayMessage = newOverlayMessage;
				Gui.this.display.timerExec(Gui.OVERLAY_TIME, new Runnable() {

					@Override
					public void run() {
						//delete the added overlay message after Gui.OVERLAY_TIME milliseconds
						//assert Gui.this.currentOverlayMessage.startsWith(overlayMessage)
						String withoutOverlayText = Gui.this.currentOverlayMessage.substring(overlayMessageToAdd.length());
						// == Gui.this.currentOverlayMessage.replaceFirst(overlayMessageToAdd, "");

						//remove the newline if there is one
						if (!withoutOverlayText.isEmpty()) {
							//assert withoutOverlayText.startswith(System.getProperty("line.seperator");
							withoutOverlayText = withoutOverlayText.substring(System.getProperty("line.separator").length());
						}

						//set the text with the removed overlay
						Gui.this.vid.setOverlay(withoutOverlayText);
						Gui.this.currentOverlayMessage=withoutOverlayText;
					}
				});

			}
		});
	}

	private void stopRecorder(MotionRecorder recorder) {

		//stop the recorder and remove it from the known recorder list
		recorder.stop();
		recorder.removeMotionRecorderListener(this.listener_pipe);
		//delete the recorder from the known recorders
		this.pipeList.remove(recorder);

		//check if it is the current visible recorder and if, delete the video window out of it
		if (recorder == this.current_recorder) {
			this.current_recorder.setPlayer(this.vid, false, true);
			// set the first stream if existent as new playback stream
			MotionRecorder newDisplayingRecorder = null;
			if(!this.pipeList.isEmpty()) {
				newDisplayingRecorder = this.pipeList.get(0);
				newDisplayingRecorder.setPlayer(this.vid, true, false);
			}
			this.current_recorder = newDisplayingRecorder;
		}
	}

	/**
	 * This method changes from MODE2 to MODE3 and vice versa.
	 * So it enables or disables the menu in the fullscreen mode of the gui
	 */
	private void changeFSMenu() {
		//get the two different Composites for the fullscreen part
		Control[] controls = this.fsComp.getChildren();
		assert controls.length == 2;

		// check which composite is visible at the moment and change it to the other one
		StackLayout layout = (StackLayout) this.fsComp.getLayout();

		Composite newFS;
		if (controls[0] == layout.topControl) {
			// controls[0] is visible at the moment. This means MODE2 fullscreen mode without a menu.
			// The new fullscreen mode should be mode 3
			newFS = (Composite) controls[1];
		} else {
			// controls[1] is visible: MODE3, fullscreen with shown menu. Change it to MODE2 without menu
			newFS = (Composite) controls[0];
		}
		// change the control to the new computed one and add the video panel if we are in fullscreen mode. (should always be the case, but was used to debug also in the default mode1.)
		layout.topControl = newFS;


		if (this.display.getShells()[0].getFullScreen()) {
			//fullscreen, add the video to the first first subcomposite of the composite to be shown
			Composite newVideoPlace = (Composite)newFS.getChildren()[0];
			this.vid.setParent(newVideoPlace);
			newVideoPlace.layout();
		}

		// BUGFIX: update the parent view (StackLayout) also when the subview changes in the Sub-StackLayout
		this.fsComp.layout();
	}

	/**
	 * This method creates the GUI with StackLayouts for all 3 modes.
	 * 
	 * @param shell the shell used as parent for the whole application
	 */
	private void configureGUI(Shell shell) {
		//set the titel
		shell.setText("Multimedia Systems 2012 Luleå - Lab 2 Server");

		//configure layout and minimum size of the shell
		shell.setMinimumSize(600, 400);
		StackLayout stackLayout = new StackLayout();
		shell.setLayout(stackLayout);

		//set a nice background
		this.setBackground(shell);

		//add the screen in normal mode (non fullscreen). Add also to it the video window, because it is the first composite shown.
		Composite defaultComposite = this.addDefaultScreen(shell);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		this.createVideoWindow(defaultComposite, gridData);
		this.addButtons(defaultComposite);

		//add a screen for the fullscreen (no video window at the moment, serves as possible place for the video window)
		this.createFullScreenPlace(shell);

		//set finally the top of the stacklayout and initionalize it
		stackLayout.topControl = defaultComposite;
		defaultComposite.layout();
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
		gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_CYAN));
		gc.setBackground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
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
		Composite comp = createMenubarComposit(parent);
		//set the composite, where the menubar is connected to, so that it can easily be disconnected or reconnected to during fullscreen switches
		this.menuBarDefaultComp = parent;

		//the gridlayout is used to center the buttons. Add hidden fields left and right to center...
		addHiddenField(comp);

		//create a new composite for the middle, to create boarder around all the buttons
		Composite middle = new Composite(comp, SWT.BORDER);
		middle.setLayout(new GridLayout(3, false));


		//add stop button
		final Button button_stop = new Button (middle, SWT.PUSH);
		button_stop.setText ("Stop!");

		//add play button and connect it to its action
		final Button button_play = new Button (middle, SWT.PUSH);
		button_play.setText ("Play");

		//add a record button
		final Button button_clients = new Button (middle, SWT.PUSH);
		button_clients.setText ("ConnectedClients");

		//disable stop and record at the beginning
		button_stop.setEnabled(false);
		button_clients.setEnabled(false);

		//add the actions for the buttons

		//add action to the play button
		button_play.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				//if the play button is clicked, the server should start listening
				try {
					boolean succ = Gui.this.startServer();
					if (!succ) {
						//not successfully started. Means no directory for recording chosen.
						//inform the user
						MessageBox dlg = new MessageBox(Gui.this.display.getShells()[0], SWT.OK);
						dlg.setMessage("No directory selected. Cannot record.");
						dlg.setText("Information");
						dlg.open();
						//and reset the selection on the button
						button_clients.setSelection(!button_clients.getSelection());
						// then return
						return;
					}
				} catch (IOException e1) {
					// error while starting listening
					Gui.this.handleConnectionError(e1);
				}
			}
		});

		// add the action to the stop button
		button_stop.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Gui.this.stopServer();
			}
		});

		//add action to the show connected client button
		button_clients.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				//inform the user
				Dialog_connectedClients connectedClientsDialog = new Dialog_connectedClients(Gui.this.display.getShells()[0], Gui.this.pipeList);
				connectedClientsDialog.open();
				//and reset the selection on the button
				button_clients.setSelection(!button_clients.getSelection());
				// then return
				return;
			}
		});
		//add another empty layout at the right side so that the buttons are centered. It has to grab horizontal size
		addHiddenField(comp);

		// and finally add a new line with the preference buttons
		addPreferenceButtons(comp);

		//set the buttons
		this.button_stop = button_stop;
		this.button_play = button_play;
		this.button_record = button_clients;
	}

	private void handleConnectionError(final Exception e) {
		// show message window and stop the server
		Gui.this.display.asyncExec(new Runnable() {

			@Override
			public void run() {
				MessageBox msgBox = new MessageBox(Gui.this.display.getShells()[0], SWT.OK|SWT.ERROR);
				msgBox.setMessage("Error whith the tcp sockets:'" + e.getMessage()+ "'.");
				msgBox.setText("Error");
				msgBox.open();
			}
		});
		//stop the server
		Gui.this.stopServer();
	}

	/** Adds prefernce buttons to the given composite
	 * 
	 * @param comp used as parent for the preference buttons
	 */
	private void addPreferenceButtons(Composite comp) {
		//center right, add a hidden Field
		Composite pref = new Composite(comp, SWT.NONE);
		pref.setLayout(new GridLayout(2,false));
		GridData data = new GridData(SWT.FILL, SWT.NONE, true, false);
		data.horizontalSpan=3;
		pref.setLayoutData(data);

		addHiddenField(pref);

		//create a sub Composit for this buttons
		Composite middle = new Composite(pref, SWT.BORDER);
		middle.setLayout(new GridLayout(3, false));

		//add a show fps button
		final Button button_fps = new Button (middle, SWT.TOGGLE);
		button_fps.setText ("showFPS");

		//add a fullscreen button
		final Button button_fullScreen = new Button (middle, SWT.TOGGLE);
		button_fullScreen.setText ("Fullscreen");

		//add a record button
		final Button button_dir = new Button (middle, SWT.PUSH);
		button_dir.setText ("Select rec. dir...");

		//add action to the show fps button
		button_fps.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//get the selection and turn fps on or of depending on the selection
				boolean selected = button_fps.getSelection();
				Gui.this.setShowFPS(selected);
			}
		});

		//add the action to the fullscren button
		button_fullScreen.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//change the fullscreen mode
				Gui.this.changeFullScreen();
			}
		});

		//add the action for the dir button
		button_dir.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//change the fullscreen mode
				Gui.this.setCapturingPath();
			}
		});
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

	/** This method is used for creating the menu bar that can be moved around 
	 * 
	 * @param parent the parent, where the composite for the menubar should be created
	 * @return Composite return the created menubar composite
	 */
	private Composite createMenubarComposit(Composite parent) {
		//first create new composite and add a grid layout to but the buttons on
		Composite comp = new Composite (parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout(3, false);
		comp.setLayout(gridLayout);

		//set the griddata to be filled horizontaly. Are used from the parent composite
		GridData gridData = new GridData(SWT.FILL, SWT.NONE, true, false);
		comp.setLayoutData(gridData);
		return comp;
	}

	/**
	 * This method is used to create a Video window with the given layoutData for the parent composite on the given parent.
	 * As side effects are the variables {@link #defaultVidComp} and {@link #vid} set to the created composite and video component.
	 * 
	 * @param parent defines the place where the video window is added
	 * @param layoutData defines how the window behaves in the parent layout
	 */
	private void createVideoWindow(Composite parent, Object layoutData) {
		//add new composite for the video window and set it to fill layout
		Composite vidComposite = new Composite(parent, SWT.NONE);
		vidComposite.setLayout(new FillLayout());

		// set the layout data to the given one. This should be from the same type then the layout requires in the given parent composite
		vidComposite.setLayoutData(layoutData);

		//create the video component and set its default behaviour
		VideoComponent vid = new VideoComponent(vidComposite, SWT.BORDER);
		vid.setKeepAspect(true);
		vid.setOverlay(this.currentOverlayMessage);
		vid.showOverlay(true);
		vid.addKeyListener(listener_keypress);

		//TODO
		//add a rightlick menu to the video window
		/*Menu menu = new Menu(vid);
		MenuItem item = new MenuItem (menu, SWT.NONE);
		item.setText("test");
		vid.setMenu(menu);
		 */

		//set the video composite as variable to be able to access it during fullscreen changes
		this.defaultVidComp = vidComposite;

		//finally set the vid to the GUI-Variable
		this.vid = vid;
	}

	/**
	 * Creates one composite as container for the two different Composites for the fullscreen.
	 * They can be switched also using the StackLayout of SWT.
	 * Side effects: 1. changes the variable {@link #fsComp} to the StackLayout-Composite of the fullscreen mode.
	 * 				 2. sets the composite {@link #menuBarFSComp} that is used to connect the menubar to.
	 * @param shell the parent, where the fullscreen composite should be added
	 */
	private void createFullScreenPlace(Shell shell) {
		//create new Composite and set the StackLayout
		Composite fullScreenComposite = new Composite(shell, SWT.NONE);
		StackLayout lay = new StackLayout();
		fullScreenComposite.setLayout(lay);

		//add the subcomposites for this Stacklayout. First without menu, second with menu

		//First the fullscreen composite without menu
		Composite withoutMenu = new Composite(fullScreenComposite, SWT.NONE);
		//to make id more adabdaple we used this sub-composite.
		// You can also use directly a fill layout.
		// But so it has the same structure than the second subcomposite in the Stacklayout, what means, that the video parts can be accessed on the same way (getChild.getChild)
		withoutMenu.setLayout(new GridLayout(1, false));
		Composite vidWithoutMenuComp = new Composite(withoutMenu, SWT.NONE);
		vidWithoutMenuComp.setLayout(new FillLayout());
		vidWithoutMenuComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		//finally addd the mouse move-listener to the created composite, in order to be able to change when the mouse moves towards the edges
		withoutMenu.addMouseMoveListener(this.listener_mouseMove_woMen);

		//Second create the Part with menu. It is nearly the same, but added the buttons at the bottom
		Composite withMenu = new Composite(fullScreenComposite, SWT.NONE);
		withMenu.setLayout(new GridLayout(1, false));
		Composite withMenuVidComp = new Composite(withMenu, SWT.NONE);
		withMenuVidComp.setLayout(new FillLayout());
		withMenuVidComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		//set the withMenu as component used for connecting/deconnecting the menubar when switching from fullscreen to default and vice versa
		this.menuBarFSComp=withMenu;
		//finally add the listener for the fullscreen WITH menu. It will update the menu_off_timer when the mouse is moved.
		withMenu.addMouseMoveListener(this.listener_mouseMove_wMen);

		// set the first fullscreen composite to MODE2: withoud menu
		lay.topControl=withoutMenu;
		withoutMenu.layout();

		//finally set the created fullscreen composite to be able to access it when a fullscreen change should happen
		this.fsComp = fullScreenComposite;
	}

	/**
	 * Adds the default screen for MODE1 to the given shell. Does not set buttons, because first the video window have to be added.
	 * @param shell parent used for the creation of this composite
	 * @return the created composite with a gridlayout with 1 column. (like row, but different sizes per row possible)
	 */
	private Composite addDefaultScreen(Shell shell) {
		//create the default screen in a new composite
		Composite defaultScreenComposite = new Composite(shell,SWT.NONE);
		//set the layout to gridlayout, like row, but single cell(here row) can be individually resized
		Layout layout = new GridLayout(1, false);
		defaultScreenComposite.setLayout(layout);

		//finally return the created composite
		return defaultScreenComposite;
	}

	/**
	 * If in Fullscreen mode, will this method change to default mode MODE1.
	 * If in default mode, it will change to the last seen FullScreen Mode. MODE2 or MODE3.
	 * On first call changes to MODE2 (without menu)
	 */
	private void changeFullScreen() {
		//get the current mode
		Shell shell = this.display.getActiveShell();
		boolean isFullScreen = shell.getFullScreen();

		//calculate the new place for the video, and the new composite to be shown in the StackLayout of the main window.
		Composite newVideoPlace;
		Composite newTopComponent;

		if (isFullScreen) {
			// currentlay in fullscreen. Change to default mode MODE1
			newVideoPlace = this.defaultVidComp;
			newTopComponent = newVideoPlace.getParent(); //parent of the default video is the default part in the StackLayout
			//delete the mousemovelistener. has no effekt if the listener is not connected. Connected only in fullscreen with menu!
			this.vid.removeMouseMoveListener(listener_mouseMove_wMen);
			//stop the timer to hide the display. No effekt if it was not running (MODE2)
			this.display.timerExec(-1, menu_timer);
			// connect the menu bar back to the default panel
			this.menuBarFSComp.getChildren()[1].setParent(this.menuBarDefaultComp);
		} else {
			//default mode 1. Change to MODE2 or MODE3, depending on with MODE was the last used fullscreen mode
			StackLayout currentFSLayout = (StackLayout) this.fsComp.getLayout();
			//get last used
			Composite currentFS = (Composite) currentFSLayout.topControl;
			//check if it was with menu, if yes, restart the timer and adds movelistener to the video part
			if (currentFS == this.fsComp.getChildren()[1]) {
				vid.addMouseMoveListener(this.listener_mouseMove_wMen);
				this.display.timerExec(this.menu_off_time, this.menu_timer);
			}
			// get the composite for the video part
			newVideoPlace = (Composite)currentFS.getChildren()[0];
			// get the new TopComponent for the StackLayout. getParent will return the Composite with or without menu, and thats parent is the Sub-StackLayout-composite
			newTopComponent = newVideoPlace.getParent().getParent();
			assert newTopComponent == this.fsComp;
			// connect the menubar to the fullscreen window for MODE3
			this.menuBarDefaultComp.getChildren()[1].setParent(this.menuBarFSComp);
		}

		//connect the video window to the new computed place
		this.vid.setParent(newVideoPlace);

		//get the layout and set the new top of the Stack.
		StackLayout layout = (StackLayout)shell.getLayout();
		layout.topControl = newTopComponent;
		newTopComponent.layout();

		//finally set/delete the real fullscreen on the shell
		shell.setFullScreen(!isFullScreen);
	}

	/** trys to get the capturing paths
	 * 
	 * @return if the setting of the capturing path was successfull
	 */
	private boolean setCapturingPath() {
		DirectoryDialog findDirectory = new DirectoryDialog(display.getActiveShell());
		findDirectory.setText("Recording directory selection");
		findDirectory.setMessage("Select the directory where the recordings should be made");
		findDirectory.setFilterPath(CapturingPath);
		String rec_dir = findDirectory.open();
		if (rec_dir == null) {
			// cancelled
			return false;
		}

		assert rec_dir != null;
		CapturingPath=rec_dir+System.getProperty("file.separator");
		return true;
	}

	/**
	 * Start/restart the recorder. After that the webcame is played, but nothing recorded.
	 * @throws IOException 
	 */

	private boolean startServer() throws IOException {

		if (CapturingPath == null) {
			boolean succ= this.setCapturingPath();
			if (!succ) {
				return false;
			}
		}

		final ConnectionManager manager = new ConnectionManager(Gui.SERVER_PORT);
		manager.addConnectionListener(this.listener_connectionManager);
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					manager.start();
				} catch (IOException e) {
					Gui.this.handleConnectionError(e);
				}
			}
		}).start();

		Gui.this.display.asyncExec(new Runnable() {

			@Override
			public void run() {
				Gui.this.setButtonsStarted();
			}
		});
		this.currentConnectionManager = manager;
		return true;
	}

	private void setButtonsStarted() {
		//set button enablements
		button_play.setEnabled(false);
		button_stop.setEnabled(true);
		button_record.setEnabled(true);

		//set the playbutton background color
		button_play.setBackground(Gui.this.display.getSystemColor(SWT.COLOR_GREEN));
	}

	/**
	 * stops the server. This will stop the connectionManager and every running pipe. It will also disconnect the video component from the current displaying pipe.
	 */
	private void stopServer() {

		//disconnect the video window from its current pipeline
		if (this.current_recorder != null) {
			this.current_recorder.setPlayer(this.vid, false, false);
		}
		// stop the connection manager 
		if (this.currentConnectionManager != null) {
			try {
				this.currentConnectionManager.stop();
				this.currentConnectionManager = null;
			} catch (IOException e) {
				// error while stopping the connection manager
				Gui.this.handleConnectionError(e);
				Gui.this.setButtonsStarted();
			}
		}

		//stop all pipelines
		for (MotionRecorder rec:this.pipeList) {
			rec.stop();
			rec.removeMotionRecorderListener(this.listener_pipe);
		}
		// and empty the pipelist
		this.pipeList.clear();

		//set the button enablement
		this.setButtonsStopped();

	}

	private void setButtonsStopped() {
		//set button enablements
		if (!this.button_play.isDisposed()) {
			this.button_play.setEnabled(true);
			//set button color back
			this.button_play.setBackground(Gui.this.display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		}

		if (!this.button_stop.isDisposed()) {

			this.button_stop.setEnabled(false);
		}

		if (!this.button_record.isDisposed()) {
			this.button_record.setEnabled(false);
			this.button_record.setSelection(false);

			//set button color back
			this.button_record.setBackground(Gui.this.display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
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

		//diposed command received: Stop the recorder, free the background image and close the display
		this.stopServer();
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

	/**
	 * Changes depending on the given boolean the overlay of the video component and shows/disables the FPS in the video part of the gui.
	 *
	 * @param useFPS boolean to set fps as overlay if true, or disables fps if false
	 */
	private void setShowFPS(boolean useFPS) {
		this.vid.showFPS(useFPS);
	}

}
