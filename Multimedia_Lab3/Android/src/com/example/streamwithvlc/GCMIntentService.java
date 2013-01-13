package com.example.streamwithvlc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import server.connectionManager.ConnectionEventType;
import server.connectionManager.ConnectionMessages;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

public class GCMIntentService extends GCMBaseIntentService {

	private static final int MAX_ATTEMPTS = 5;

	public GCMIntentService(){
		super(MainActivity.SENDER_ID);
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		// TODO Auto-generated method stub
		//TODO payoad is a bundle: intent.getExtras();

	}

	@Override
	protected void onError(Context context, String errorId) {
		// TODO Typically, there is nothing to be done other than evaluating the error (returned by errorId) and trying to fix the problem.

	}

	@Override
	protected void onRegistered(Context context, String registrationId) {
		// TODO send registrationId to server so that he can send messages to this application
		//TODO send registration ID to a socket and the server
		for (int i = 1; i <= MAX_ATTEMPTS; i++) {
			try{
				PrintWriter pw = null;
				Scanner scanner = null;
				Socket socket = null;
				try {
					//creates a socket
					socket = new Socket(this.getHostName(), this.getPort());
					InputStream in = socket.getInputStream();
					OutputStream out = socket.getOutputStream();
					pw = new PrintWriter(out);
					
					//first try to get the socket. Like defined in the procol send also the own properties
					pw.write(ConnectionEventType.REGISTER.name()+"\n");
					pw.write(registrationId + "\n");
					pw.flush();

					//then wait for an answer and check if it is true or false
					scanner = new Scanner(in);
					if (!scanner.hasNext()) {
						throw new IOException("Server did not answer!");
					}
					if (!scanner.hasNextInt()) {
						String answer = "";
						if (scanner.hasNextLine()) {
						answer = scanner.nextLine();
						}else {
							while (scanner.hasNext()) {
								answer = answer + " " + scanner.next();
							}
						}
						
						if (ConnectionMessages.valueOf(scanner.next()) != ConnectionMessages.SERVER_EXCEPTION) {
							//unknown answer received
							throw new IOException("Server didn't answer correct. Answer was '" + answer + "'.");
						} else {
							//EXCEPTION. Means that the server
							throw new IOException("Room '" + this.getRoom() + "' has already a camera with the name '" + this.getCameraName() + "'. Please rename it. Connection aborted.");
						}
					}
					
					//everything is fine. Read the port and return it
					int remote_port = scanner.nextInt();
					return remote_port;
				} finally {
					//finally close all streams etc
					if(pw!=null) {

						pw.close();
					}
					if (scanner!=null) {
						scanner.close();	
					}
					if (socket!=null){
						socket.close();
					}
				}
			}
		    GCMRegistrar.setRegisteredOnServer(context, true);
			//sleep
			//backoff *=2 exponential backoff
		}

	}

	@Override
	protected void onUnregistered(Context context, String registrationId) {
		// TODO send id to server in order to unregister the device from the server
		
		 if (GCMRegistrar.isRegisteredOnServer(context)) {
	            //
	            GCMRegistrar.setRegisteredOnServer(context, false);
	        } else {
	            // not registered
	            Log.i("ANSURGCM", "Ignoring unregister callback");
	        }

	}
	
	@Override
	protected boolean onRecoverableError(Context context, String errorId) {
		/** Called when the device tries to register or unregister, but the GCM servers are unavailable.
		 * The GCM library will retry the operation using exponential backup, unless this method is 
		 * overridden and returns false.
		 * This method is optional and should be overridden only if you want to display the message to
		 * the user or cancel the retry attempts.
		 */
		return super.onRecoverableError(context, errorId);
	}
}