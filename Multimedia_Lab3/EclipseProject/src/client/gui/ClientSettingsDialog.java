package client.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import client.data.RecorderHost;


class ClientSettingsDialog {
	private Shell shell;
	private RecorderHost recorderHost;
	private RecorderHost oldRecorderHost;

	ClientSettingsDialog(Shell parent, RecorderHost oldHost) {
		this.shell=parent;
		this.oldRecorderHost = oldHost;
		this.recorderHost=null;
	}

	RecorderHost open() {
		final Shell dialog = new Shell(this.shell, SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM);
		dialog.setLayout(new GridLayout(2, false));
		dialog.setText("Settings");
		dialog.setSize(400, 150);

		Label label_Host = new Label(dialog, SWT.NONE);
		label_Host.setText("Host");
		label_Host.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		final Text text_HostName = new Text(dialog, SWT.SINGLE | SWT.BORDER);
		text_HostName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

		text_HostName.setText(this.oldRecorderHost.getHostName());

		final Label label_Port = new Label(dialog, SWT.NONE);
		label_Port.setText("Port");
		label_Port.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		final Text text_Port = new Text(dialog, SWT.SINGLE | SWT.BORDER);

		int oldPort = this.oldRecorderHost.getPort();
		text_Port.setText("" + oldPort);

		text_Port.setTextLimit(5);
		GridData gridData_Port = new GridData(SWT.LEFT, SWT.CENTER, false, true);
		gridData_Port.widthHint = 50;
		text_Port.setLayoutData(gridData_Port);
		text_Port.addVerifyListener(new VerifyListener() {  

			@Override  
			public void verifyText(final VerifyEvent event) {  
				switch (event.keyCode) {  
				case SWT.BS:           // Backspace  
				case SWT.DEL:          // Delete  
				case SWT.HOME:         // Home  
				case SWT.END:          // End  
				case SWT.ARROW_LEFT:   // Left arrow  
				case SWT.ARROW_RIGHT:  // Right arrow  
					return;  
				}  

				if (!Character.isDigit(event.character)) {  
					event.doit = false;  // disallow the action  
				}  
			}  

		});

		final Button button_OK = new Button(dialog, SWT.PUSH);
		button_OK.setText("OK");

		Button button_Cancel = new Button(dialog, SWT.PUSH);
		button_Cancel.setText("Cancel");

		SelectionListener listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (event.widget == button_OK) {
					String portString = text_Port.getText();
					int port;
					if (portString.isEmpty()) {
						port = ClientSettingsDialog.this.oldRecorderHost.getPort();
					} else {
						port = new Integer(portString).intValue();
					}
					String hostName = text_HostName.getText();

					if (hostName.trim().isEmpty()) {
						hostName = ClientSettingsDialog.this.oldRecorderHost.getHostName();
					}
					System.out.println(port);
					System.out.println(hostName);
					ClientSettingsDialog.this.recorderHost = new RecorderHost(hostName, port);
				}
				dialog.close();
			}
		};

		button_OK.addSelectionListener(listener);
		button_Cancel.addSelectionListener(listener);

		dialog.open();

		while (!dialog.isDisposed()) {
			Display display = dialog.getDisplay();
			if (!display.readAndDispatch())
				display.sleep();
		}

		return this.recorderHost;
	}


}
