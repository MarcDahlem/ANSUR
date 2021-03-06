package com.example.streamwithvlc;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import classes.Camera;
import classes.Room;

import com.google.android.gcm.GCMRegistrar;
import commonUtility.ConnectionEventType;

public class AppConnectionManager {

	private static final int MAX_ATTEMPTS = 5;
	private static final int BACKOFF_MILLI_SECONDS = 2000;
	private static final Random random = new Random();

	public static boolean register(final Context context, String registrationId) {
		String message = "Registering device (regId = " + registrationId + ")";
		Log.i("ANSURGCM", message);

		MainActivity.displayMessage(context, message);

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
						String notAnswerMessage = "Server did not answer!";
						MainActivity.displayMessage(context, notAnswerMessage);
						throw new IOException(notAnswerMessage);
					}

					String line = scanner.nextLine();

					//everything is fine. Read the answer
					if ("true".equals(line)) {

						String successMessage = "Device successfully (un)registered on server (regId = " + registrationId + ")";
						MainActivity.displayMessage(context, successMessage);
						Log.i("ANSURGCM", successMessage);
						return true;
					} else {
						if ("false".equals(line)) {
							String message = "Registration: Device is allready registered on server. Deregistration: Device was not registered.";
							MainActivity.displayMessage(context, message);
							return true;
						} else {
							if (ConnectionEventType.SERVER_EXCEPTION.name().equals(line)) {
								String message = "Server answered with an exception.";
								MainActivity.displayMessage(context, message);
								throw new IOException(message);
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
		String message = "unregistering device (regId = " + registrationId + ")";
		Log.i("ANSURGCM", message);
		MainActivity.displayMessage(context, message);

		ConnectionEventType type = ConnectionEventType.CLIENT_DEREGISTER;

		//unregister will also unsubscribe on the server side. dont need to think about it here

		boolean success = postBooleanServerCommand(context, registrationId, type);
		if (success) {
			GCMRegistrar.setRegisteredOnServer(context, false);
		}
		return success;
	}

	public static Collection<Room> getAllCameras(Context context, String regId) throws IOException {
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
			pw.write(regId+"\n");
			pw.flush();

			//then wait for the answer and restructure it
			scanner = new Scanner(in);
			if (!scanner.hasNextLine()) {
				String message = "Server did not answer!";
				MainActivity.displayMessage(context, message);
				throw new IOException(message);
			}

			if (!scanner.hasNextInt()) {
				String line = scanner.nextLine();
				String message = "Server did not answer correctly! Expected number of cameras, got '"+ line + "'.";
				MainActivity.displayMessage(context, message);
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
					MainActivity.displayMessage(context, message);
					throw new IOException(message);
				}

				if (!scanner.hasNextInt()) {
					String line = scanner.nextLine();
					String message = "Server did not answer correctly! Expected port of camera, got '"+ line + "'.";
					MainActivity.displayMessage(context, message);
					throw new IOException(message);
				}

				int port = scanner.nextInt();
				//skip the linebreak
				scanner.nextLine();
				if (!scanner.hasNextLine()) {
					String message = "No room name for camera " + (i+1) +"/" + amount + " on port " + port +".";
					MainActivity.displayMessage(context, message);
					throw new IOException(message);
				}

				String roomName = scanner.nextLine();

				if (!scanner.hasNextLine()) {
					String message = "No camera name for camera " + (i+1) +"/" + amount + " on port " + port +" in room "+ roomName+".";
					MainActivity.displayMessage(context, message);
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

				if (!scanner.hasNextLine()) {
					String message = "No camera subscribtion status found for camera " + (i+1) +"/" + amount + " with name "+ cameraName+ " on port " + port +" in room "+ roomName+".";
					MainActivity.displayMessage(context, message);
					throw new IOException(message);
				}

				if (!scanner.hasNextBoolean()) {
					String line = scanner.nextLine();
					String message = "Server did not answer correctly! Expected subscription status of camera, got '"+ line + "'.";
					MainActivity.displayMessage(context, message);
					throw new IOException(message);
				}

				//read the subscribtion status
				boolean subscribed = scanner.nextBoolean();
				scanner.nextLine();

				//add the camera to this room
				Camera cam = new Camera(cameraName, port, subscribed);
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
				String message = "Server did not answer!";
				MainActivity.displayMessage(context, message);
				throw new IOException(message);
			}

			String answer = scanner.nextLine();

			if (ConnectionEventType.SERVER_EXCEPTION.name().equals(answer)) {
				String message = "Server answered with an exception. It can be that one client to subscribe to is not available.";
				MainActivity.displayMessage(context, message);
				throw new IOException(message);
			}

			if ("done".equals(answer)) {
				Log.i("ANSUR", "Subscribed to all " + cameras.size() + " cameras.");
				// at the end return all collected rooms
				return;
			} else {
				String message = "Serveranswer is unknown. '" + answer + "'.";
				MainActivity.displayMessage(context, message);
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
				String message = "Server did not answer!";
				MainActivity.displayMessage(context, message);
				throw new IOException(message);
			}

			String answer = scanner.nextLine();

			if (ConnectionEventType.SERVER_EXCEPTION.name().equals(answer)) {
				String message = "Server answered with an exception. It can be that one client to subscribe to is not available.";
				MainActivity.displayMessage(context, message);
				throw new IOException(message);
			}

			if ("done".equals(answer)) {
				Log.i("ANSUR", "Unsubscribed from all " + cameras.size() + " cameras.");
				// at the end return all collected rooms
				return;
			} else {
				String message = "Serveranswer is unknown. '" + answer + "'.";
				MainActivity.displayMessage(context, message);
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


	public static String downloadMotionRecord(final Context context, String filename) throws UnknownHostException, IOException {
		PrintWriter pw = null;
		Scanner scanner = null;
		Socket socket = null;
		Log.i("ANSUR", "Trying to download movie...");
		try {
			//create a socket
			socket = new Socket(MainActivity.HOSTNAME, MainActivity.PORT);
			InputStream in = socket.getInputStream();
			OutputStream out = socket.getOutputStream();
			pw = new PrintWriter(out);

			//Send the get unsubscribtion command like defined in the protocol
			pw.write(ConnectionEventType.CLIENT_DOWNLOAD_MOTION.name()+"\n");
			pw.write(filename+"\n");
			pw.flush();

			//then wait for the answer
			scanner = new Scanner(in);
			if (!scanner.hasNextLine()) {
				String message = "Server did not answer!";
				MainActivity.displayMessage(context, message);
				throw new IOException(message);
			}

			if (!scanner.hasNextInt()) {
				String answer = scanner.nextLine();
				if (ConnectionEventType.SERVER_EXCEPTION.name().equals(answer)) {
					String message = "Server answered with an exception. Either IOException on the server side or filename not valid";
					MainActivity.displayMessage(context, message);
					throw new IOException(message);
				} else {
					String message = "Server answered not with the filesize, but with '" + answer+ "'! Not conform to the protocol.";
					MainActivity.displayMessage(context, message);
					throw new IOException(message);
				}
			}

			final int filesize = scanner.nextInt();
			scanner.nextLine();

			if (!scanner.hasNextLine()) {
				String message = "Server did not send the filename! Not conform to the protocol.";
				MainActivity.displayMessage(context, message);
				throw new IOException(message);
			}

			final String newFilename = scanner.nextLine();

			// read the port for the download
			if (!scanner.hasNextLine()) {
				String message = "Server did not send the download port! Not conform to the protocol.";
				MainActivity.displayMessage(context, message);
				throw new IOException(message);
			}

			if (!scanner.hasNextInt()) {
				String answer = scanner.nextLine();
				String message = "Server answered not with the donwload port, but with '" + answer+ "'! Not conform to the protocol.";
				MainActivity.displayMessage(context, message);
				throw new IOException(message);
			}

			final int downloadPort = scanner.nextInt();

			//start downloading
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						AppConnectionManager.startRealDownload(downloadPort, context, filesize, newFilename);
					} catch (IOException e) {
						Log.e("ANSUR.connection", "Downloading movie failed: ",e);
						MainActivity.displayMessage(context, e.getMessage());
					}
				}
			}).start();

			return newFilename;
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

	private static void startRealDownload(int downloadPort, Context context, int filesize, String filename) throws IOException {
		BufferedOutputStream bufferedOutputStream=null;
		Socket socket = null;
		InputStream in = null;
		try{
			//create a socket
			socket = new Socket(MainActivity.HOSTNAME, downloadPort);
			in = socket.getInputStream();

			//read the file itself

			File file = new File(context.getExternalFilesDir(null), filename);
			String fullPathName = file.getAbsolutePath();
			
			//create starting information
			String startMessage = "Trying to download movie to " + fullPathName;	
			handleMessage(context, startMessage);
			
			createDownloadInformation(context, false, fullPathName, 0, filesize);
			
			//download the file
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

			int currentReadLength;
			byte[] movieArray;
			int size = 1024; //download the movie in steps of 1024 bytes
			int total=0;

			if (in instanceof ByteArrayInputStream) {
				size = in.available();
				movieArray = new byte[size];
				currentReadLength = in.read(movieArray, 0, size);
			} else {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				movieArray = new byte[size];
				while ((currentReadLength = in.read(movieArray, 0, size)) != -1) {
					bos.write(movieArray, 0, currentReadLength);
					total+=currentReadLength;
					createDownloadInformation(context, false, fullPathName, total, filesize);
				}
				movieArray = bos.toByteArray();
			}



			if (movieArray.length!=filesize) {
				String message = "Downloading error. Received bytes = " + movieArray.length + ", expected bytes =" +filesize+".";
				throw new IOException(message);
			}
			//			for (int i=0;i<bytearray.length;i++) {
			//				in.read(bytearray);
			//				if (!scanner.hasNextByte()) {
			//					String message = "Bytestream stopped at Byte " + (i+1) +"/"+bytearray.length+" before the file was fully received.";
			//					MainActivity.displayMessage(context, message);
			//					throw new IOException(message);
			//				}
			//				bytearray[i]=scanner.nextByte();
			//			}

			bufferedOutputStream.write(movieArray, 0 , movieArray.length);
			bufferedOutputStream.flush();
			createDownloadInformation(context, true, fullPathName, movieArray.length, filesize);
			handleMessage(context, "Downloading movie finished!");
		} finally {
			if (bufferedOutputStream !=null) {
				bufferedOutputStream.close();
			}

			if (in!=null) {
				in.close();
			}
		}
	}

	private static void createDownloadInformation(Context context, boolean isDone, String filepath, int finished, int total) {
		Log.i("ANSUR.Connection", "Download update: isdone=" + isDone + ", finished " + finished + "/" + total +"bytes.");
		Intent intent = new Intent(DownloadFileActivity.DOWNLOAD_MESSAGE_ACTION);
        intent.putExtra(DownloadFileActivity.DMessage_ISDONE, isDone);
        intent.putExtra(DownloadFileActivity.DMessage_MOVIEPATH, filepath);
        intent.putExtra(DownloadFileActivity.DMessage_BYTESDOWNLOADED, finished);
        intent.putExtra(DownloadFileActivity.DMessage_FILESIZE, total);
        context.sendBroadcast(intent);
	}

	private static void handleMessage(Context context, String startMessage) {
		Log.i("ANSUR.Connection", startMessage);
		Intent intent = new Intent(MainActivity.TOAST_MESSAGE_ACTION);
        intent.putExtra(MainActivity.EXTRA_MESSAGE, startMessage);
        context.sendBroadcast(intent);
	}
}
