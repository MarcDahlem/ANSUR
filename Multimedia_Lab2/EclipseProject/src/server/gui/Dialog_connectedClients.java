package server.gui;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

import server.motionRecorder.MotionRecorder;


class Dialog_connectedClients {
	private Shell shell;
	private ArrayList<MotionRecorder> clientList;

	Dialog_connectedClients(Shell parent, ArrayList<MotionRecorder> clientList) {
		this.shell=parent;
		this.clientList = clientList;
	}

	void open() {
		final Shell dialog = new Shell(this.shell, SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM);
		dialog.setLayout(new GridLayout(2, false));
		dialog.setText("Connected clients");
		dialog.setSize(400, 400);

		List clientList = new List(dialog, SWT.NONE|SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL);
		for (MotionRecorder recorder: this.clientList) {
			clientList.add(recorder.getName());
		}
		clientList.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

		final Button button_OK = new Button(dialog, SWT.PUSH);
		button_OK.setText("OK");
		
		button_OK.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				dialog.close();
			}
		});

		dialog.open();

		while (!dialog.isDisposed()) {
			Display display = dialog.getDisplay();
			if (!display.readAndDispatch())
				display.sleep();
		}

		return;
	}
}
