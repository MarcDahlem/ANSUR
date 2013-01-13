package com.example.streamwithvlc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;
import java.util.Scanner;

import android.content.Context;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;
import commonUtility.ConnectionEventType;

public class ConnectionManager {

	private static final int MAX_ATTEMPTS = 5;
	private static final int BACKOFF_MILLI_SECONDS = 2000;
	private static final Random random = new Random();

	public static boolean register(final Context context, String registrationId) {
		Log.i("ANSURGCM", "Registering device (regId = " + registrationId + ")");
		ConnectionEventType type = ConnectionEventType.CLIENT_REGISTER;

		boolean success = postBooleanServerCommand(context, registrationId, type);
		if (success) {
			GCMRegistrar.setRegisteredOnServer(context, true);
		}
		return success;
	}

	private static boolean postBooleanServerCommand(final Context context,
			String registrationId, ConnectionEventType type) {
		long backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);

		for (int i = 1; i <= MAX_ATTEMPTS; i++) {
			try{
				PrintWriter pw = null;
				Scanner scanner = null;
				Socket socket = null;
				try {
					//creates a socket
					socket = new Socket(MainActivity.HOSTNAME, MainActivity.PORT);
					InputStream in = socket.getInputStream();
					OutputStream out = socket.getOutputStream();
					pw = new PrintWriter(out);

					//first try to get the socket. Like defined in the procol send also the own properties
					pw.write(type.name()+"\n");
					pw.write(registrationId + "\n");
					pw.flush();

					//then wait for an answer and check if it is true or false
					scanner = new Scanner(in);
					if (!scanner.hasNextLine()) {
						throw new IOException("Server did not answer!");
					}

					String line = scanner.nextLine();

					//everything is fine. Read the answer
					if ("true".equals(line)) {
						Log.i("ANSURGCM", "Device successfully (un)registered on server (regId = " + registrationId + ")");
						return true;
					} else {
						if ("false".equals(line)) {
							//TODO message that it is already registered
							return true;
						} else {
							if (ConnectionEventType.SERVER_EXCEPTION.name().equals(line)) {
								throw new IOException("Server answered with an exception.");
							}
							throw new IOException("Server answered with an unknown answer '"+line+"'.");
						}
					}
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
			} catch (IOException e) {
				Log.e("ANSURGCM", "Failed to register on attempt " + i, e);
				if (i == MAX_ATTEMPTS) {
					Log.e("ANSURGCM", "MAX_ATTEMPTS reached. (Un)Registering aborted");
					break;
				}
				try {
					Log.d("ANSURGCM", "Sleeping for " + backoff + " ms before retry");
					Thread.sleep(backoff);
				} catch (InterruptedException e1) {
					// Activity finished before we complete - exit.
					Log.d("ANSURGCM", "Thread interrupted: abort remaining retries!");
					Thread.currentThread().interrupt();
					return false;
				}
				// increase backoff exponentially
				backoff *= 2;
			}
		}

		//not successfully (un)registered
		return false;
	}

	public static boolean unregister(Context context, String registrationId) {
		Log.i("ANSURGCM", "unregistering device (regId = " + registrationId + ")");

		ConnectionEventType type = ConnectionEventType.CLIENT_DEREGISTER;

		boolean success = postBooleanServerCommand(context, registrationId, type);
		if (success) {
			GCMRegistrar.setRegisteredOnServer(context, false);
		}
		return success;
	}

}
