package com.example.streamwithvlc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import classes.Camera;
import classes.Room;

import com.google.android.gcm.GCMRegistrar;
import commonUtility.ConnectionEventType;

public class ConnectionManager {

	private static final int MAX_ATTEMPTS = 5;
	private static final int BACKOFF_MILLI_SECONDS = 2000;
	private static final Random random = new Random();

	public static boolean register(final Context context, String registrationId) {
		Log.i("ANSURGCM", "Registering device (regId = " + registrationId + ")");

		Toast.makeText(context, "Registering device (regId = " + registrationId + ")",
				Toast.LENGTH_SHORT).show();

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
						Toast.makeText(context, "Server did not answer!",
								Toast.LENGTH_SHORT).show();
						throw new IOException("Server did not answer!");
					}

					String line = scanner.nextLine();

					//everything is fine. Read the answer
					if ("true".equals(line)) {
						Toast.makeText(context, "Device successfully (un)registered on server (regId = " + registrationId + ")",
								Toast.LENGTH_SHORT).show();
						Log.i("ANSURGCM", "Device successfully (un)registered on server (regId = " + registrationId + ")");
						return true;
					} else {
						if ("false".equals(line)) {
							Toast.makeText(context, "Device is allready registerd on server",
									Toast.LENGTH_SHORT).show();
							return true;
						} else {
							if (ConnectionEventType.SERVER_EXCEPTION.name().equals(line)) {
								Toast.makeText(context, "Server answered with an exception.",
										Toast.LENGTH_SHORT).show();
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
		Toast.makeText(context, "unregistering device (regId = " + registrationId + ")",
				Toast.LENGTH_SHORT).show();

		ConnectionEventType type = ConnectionEventType.CLIENT_DEREGISTER;

		//TODO unsubscribe from all subscibed cameras
		boolean success = postBooleanServerCommand(context, registrationId, type);
		if (success) {
			GCMRegistrar.setRegisteredOnServer(context, false);
		}
		return success;
	}

	public static Collection<Room> getAllCameras(Context context) throws IOException {
		PrintWriter pw = null;
		Scanner scanner = null;
		Socket socket = null;
		Log.i("ANSUR", "Trying to get all cameras");
		try {
			//creates a socket
			socket = new Socket(MainActivity.HOSTNAME, MainActivity.PORT);
			InputStream in = socket.getInputStream();
			OutputStream out = socket.getOutputStream();
			pw = new PrintWriter(out);

			//Send the get cams command like defined in the protocol
			pw.write(ConnectionEventType.CLIENT_GET_CAMS.name()+"\n");
			pw.flush();

			//then wait for the answer and restructure it
			scanner = new Scanner(in);
			if (!scanner.hasNextLine()) {
				Toast.makeText(context, "Server did not answer!", Toast.LENGTH_SHORT).show();
				throw new IOException("Server did not answer!");
			}

			if (!scanner.hasNextInt()) {
				String line = scanner.nextLine();
				String message = "Server did not answer correctly! Expected number of cameras, got '"+ line + "'.";
				Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
				throw new IOException(message);
			}
			int amount = scanner.nextInt();
			//skip the linebreak
			scanner.nextLine();

			Map<String, Room> rooms = new TreeMap<String, Room>();
			for (int i = 0; i<amount; i++) {
				//first read the port, 2nd rommname, 3rd cameraname

				if (!scanner.hasNextLine()) {
					String message = "No camera port for camera " + (i+1) +"/" + amount + ".";
					Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
					throw new IOException(message);
				}

				if (!scanner.hasNextInt()) {
					String line = scanner.nextLine();
					String message = "Server did not answer correctly! Expected port of camera, got '"+ line + "'.";
					Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
					throw new IOException(message);
				}

				int port = scanner.nextInt();
				if (!scanner.hasNextLine()) {
					String message = "No room name for camera " + (i+1) +"/" + amount + " on port " + port +".";
					Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
					throw new IOException(message);
				}

				String roomName = scanner.nextLine();

				if (!scanner.hasNextLine()) {
					String message = "No camera name for camera " + (i+1) +"/" + amount + " on port " + port +" in room "+ roomName+".";
					Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
					throw new IOException(message);
				}

				String cameraName = scanner.nextLine();

				//get the room
				Room room;
				if (rooms.containsKey(roomName)) {
					room = rooms.get(roomName);
				} else {
					room = new Room(roomName);
					rooms.put(roomName, room);
				}

				//add the camera to this room
				Camera cam = new Camera(cameraName, port, false); //TODO check with the old cameras and set them selected if they were before
				room.addCamera(cam);
			}


			Log.i("ANSUR", "All cameras sucessfully received");
			// at the end return all collected rooms
			return rooms.values();
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

	public static void subscribeTo(Context context, Collection<Camera> cameras, String registrationId) throws IOException {
		PrintWriter pw = null;
		Scanner scanner = null;
		Socket socket = null;
		Log.i("ANSUR", "Trying to subscribe to " + cameras.size() + " cameras.");
		try {
			//creates a socket
			socket = new Socket(MainActivity.HOSTNAME, MainActivity.PORT);
			InputStream in = socket.getInputStream();
			OutputStream out = socket.getOutputStream();
			pw = new PrintWriter(out);

			//Send the get subscribtion command like defined in the protocol
			pw.write(ConnectionEventType.CLIENT_SUBSCRIBE.name()+"\n");
			pw.write(cameras.size()+"\n");
			pw.write(registrationId+"\n");
			
			for (Camera camera:cameras) {
				pw.write(camera.getPort() + "\n");
			}
			pw.flush();

			//then wait for the answer
			scanner = new Scanner(in);
			if (!scanner.hasNextLine()) {
				Toast.makeText(context, "Server did not answer!", Toast.LENGTH_SHORT).show();
				throw new IOException("Server did not answer!");
			}

			String answer = scanner.nextLine();

			if (ConnectionEventType.SERVER_EXCEPTION.name().equals(answer)) {
				String message = "Server answered with an exception. It can be that one client to subscribe to is not available.";
				Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
				throw new IOException(message);
			}

			if ("done".equals(answer)) {
				Log.i("ANSUR", "Subscribed to all " + cameras.size() + " cameras.");
				// at the end return all collected rooms
				return;
			} else {
				String message = "Serveranswer is unknown. '" + answer + "'.";
				Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
				throw new IOException(message);
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

	}
	
	public static void unsubscribeFrom(Context context, Collection<Camera> cameras, String registrationId) throws IOException {
		PrintWriter pw = null;
		Scanner scanner = null;
		Socket socket = null;
		Log.i("ANSUR", "Trying to unsubscribe from " + cameras.size() + " cameras.");
		try {
			//create a socket
			socket = new Socket(MainActivity.HOSTNAME, MainActivity.PORT);
			InputStream in = socket.getInputStream();
			OutputStream out = socket.getOutputStream();
			pw = new PrintWriter(out);

			//Send the get unsubscribtion command like defined in the protocol
			pw.write(ConnectionEventType.CLIENT_UNSUBSCRIBE.name()+"\n");
			pw.write(cameras.size()+"\n");
			pw.write(registrationId+"\n");
			
			for (Camera camera:cameras) {
				pw.write(camera.getPort() + "\n");
			}
			pw.flush();

			//then wait for the answer
			scanner = new Scanner(in);
			if (!scanner.hasNextLine()) {
				Toast.makeText(context, "Server did not answer!", Toast.LENGTH_SHORT).show();
				throw new IOException("Server did not answer!");
			}

			String answer = scanner.nextLine();

			if (ConnectionEventType.SERVER_EXCEPTION.name().equals(answer)) {
				String message = "Server answered with an exception. It can be that one client to subscribe to is not available.";
				Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
				throw new IOException(message);
			}

			if ("done".equals(answer)) {
				Log.i("ANSUR", "Unsubscribed from all " + cameras.size() + " cameras.");
				// at the end return all collected rooms
				return;
			} else {
				String message = "Serveranswer is unknown. '" + answer + "'.";
				Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
				throw new IOException(message);
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

	}

}
