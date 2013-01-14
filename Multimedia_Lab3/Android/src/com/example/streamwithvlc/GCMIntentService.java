package com.example.streamwithvlc;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;
import commonUtility.GcmMessages;

public class GCMIntentService extends GCMBaseIntentService {

	public GCMIntentService(){
		super(MainActivity.SENDER_ID);
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		Bundle extras = intent.getExtras(); 
		String msg = extras.getString(GcmMessages.COMMAND);

		switch(GcmMessages.GCMCOMMAND.valueOf(msg)){
		case MOTION_START:
			String msFile_path = extras.getString(GcmMessages.FILE_PATH);
			String msCamera_name = extras.getString(GcmMessages.CAMERA_NAME);
			String msRoom_name = extras.getString(GcmMessages.ROOM_NAME);
			String msPort = extras.getString(GcmMessages.PORT);

			String msMessage = "Cam: " + msCamera_name + " in: " + msRoom_name + " on:" + msPort;
			createNotificationMotionDetected("Motion detected on", msMessage, msFile_path.hashCode());
			break;

		case MOTION_END:
			String meFile_path = extras.getString(GcmMessages.FILE_PATH);
			String meCamera_name = extras.getString(GcmMessages.CAMERA_NAME);
			String meRoom_name = extras.getString(GcmMessages.ROOM_NAME);
			String mePort = extras.getString(GcmMessages.PORT);

			String meMessage = "Download recorded motion";

			createNotificationMotionStopped("Motion detection stopped", meMessage, meFile_path.hashCode(), meFile_path);
			break;

		case CAMERA_DOWN:
			String cdCamera_name = extras.getString(GcmMessages.CAMERA_NAME);
			String cdRoom_name = extras.getString(GcmMessages.ROOM_NAME);
			String cdPort = extras.getString(GcmMessages.PORT);

			String cdMessage = "Camera " + cdCamera_name+ " in " +cdRoom_name+ " on port: " + cdPort + " was disconnected";

			Intent camIntent = new Intent(ListCamerasActivity.CAMERA_DISC_ACTION);
			camIntent.putExtra(ListCamerasActivity.CAM_DISC_ROOM, cdRoom_name);
			camIntent.putExtra(ListCamerasActivity.CAM_DISC_CAM, cdCamera_name);
			camIntent.putExtra(ListCamerasActivity.CAM_DISC_PORT, cdPort);
			context.sendBroadcast(camIntent);

			createDefaultNotification(context, "CAMERA LOST", "Lost camera", cdMessage, cdPort.hashCode());
			break;

		case SERVER_SHUTDOWN:
			//unregister
			GCMRegistrar.setRegisteredOnServer(context, false);
			GCMRegistrar.unregister(context);

			createDefaultNotification(context, "SERVER SHUTDOWN", "Server down", "The server was shut down",1);
			break;
		}

	}


	public void createNotificationMotionDetected(String title, String message, int notificationID) {

		Bitmap bMap = BitmapFactory.decodeResource(getResources(), R.drawable.motion);

		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
		.setSmallIcon(R.drawable.motion)
		.setLargeIcon(bMap)
		.setContentTitle(title)
		.setContentText(message)
		.setTicker("MOTION DETECTED");
		// Creates an explicit intent for an Activity in your app
		//Intent resultIntent = new Intent(this, MainActivity.class);

		// The stack builder object will contain an artificial back stack for the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder = stackBuilder.addParentStack(MainActivity.class);

		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder = stackBuilder.addNextIntent(launchVLC());
		PendingIntent resultPendingIntent =
				stackBuilder.getPendingIntent(
						0,
						PendingIntent.FLAG_UPDATE_CURRENT
						);
		mBuilder = mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		// notificationID allows you to update the notification later on.
		mNotificationManager.notify(notificationID, mBuilder.build());

		ClipboardManager clipboard = (ClipboardManager)
				getSystemService(Context.CLIPBOARD_SERVICE);
		ClipData clip = ClipData.newPlainText("Server","tcp://"+MainActivity.HOSTNAME+":"+MainActivity.PORT);

		clipboard.setPrimaryClip(clip);

	}

	public void createNotificationMotionStopped(String title, String message,int notificationID, String file_path ) {
		// Same as before, only that instead if going to MainActivity, you download movie through connectionmanager

		Bitmap bMap = BitmapFactory.decodeResource(getResources(), R.drawable.download_button);
		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
		.setSmallIcon(R.drawable.download_button)
		.setContentTitle(title)
		.setContentText(message)
		.setLargeIcon(bMap)
		.setAutoCancel(true)
		.setTicker("MOTION STOPPED");
		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent(this, DownloadFileActivity.class);
		resultIntent.putExtra("FILE_PATH", file_path);
		// The stack builder object will contain an artificial back stack for the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder = stackBuilder.addParentStack(MainActivity.class);

		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder = stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent =
				stackBuilder.getPendingIntent(
						0,
						PendingIntent.FLAG_UPDATE_CURRENT
						);
		mBuilder = mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		// notificationID allows you to update the notification later on.
		mNotificationManager.notify(notificationID, mBuilder.build());
	}

	public void createDefaultNotification(Context context, String ticker, String title, String message,int notificationID ) {
		// Same as before, only that instead if going to MainActivity, you download movie through connectionmanager

		Bitmap bMap = BitmapFactory.decodeResource(getResources(), R.drawable.servercrash);
		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
		.setSmallIcon(R.drawable.servercrash)
		.setContentTitle(title)
		.setContentText(message)
		.setLargeIcon(bMap)
		.setAutoCancel(true)
		.setTicker(ticker);
		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent();
		// The stack builder object will contain an artificial back stack for the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder = stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent =
				stackBuilder.getPendingIntent(
						0,
						PendingIntent.FLAG_UPDATE_CURRENT
						);
		mBuilder = mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		// notificationID allows you to update the notification later on.
		mNotificationManager.notify(notificationID, mBuilder.build());
		MainActivity.displayMessage(context, message);

	}

	public Intent launchVLC(){
		PackageManager packageManager = getApplicationContext().getPackageManager();
		Intent applicationIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);

		String vendor = "VLC Player";
		String packageName = "org.videolan.vlc.betav7neon";
		String className = "org.videolan.vlc.betav7neon.gui.MainActivity";


		boolean foundApplicationImpl = false;

		try {
			ComponentName cn = new ComponentName(packageName, className);
			ActivityInfo aInfo = packageManager.getActivityInfo(cn,
					PackageManager.GET_META_DATA);
			applicationIntent.setComponent(cn);
			Log.d("Debug", "Found " + vendor + " --> " + packageName + "/"
					+ className);
			foundApplicationImpl = true;
		} catch (NameNotFoundException e) {
			Log.d("Debug", vendor + " does not exists");
		}

		if (foundApplicationImpl) {
			applicationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			return applicationIntent;
			//startActivity(applicationIntent);
		}
		return null;
	}


	@Override
	protected void onError(Context context, String errorId) {
		// Typically, there is nothing to be done other than evaluating the error (returned by errorId) and trying to fix the problem.
		String message = "GCM error received with errorId "+errorId;
		Log.e("ANSUR.GCMIntentService", message);
		MainActivity.displayMessage(context, message);
	}

	@Override
	protected void onRegistered(Context context, String registrationId) {
		AppConnectionManager.register(context, registrationId);
	}

	@Override
	protected void onUnregistered(Context context, String registrationId) {
		// send id to server in order to unregister the device from the server
		Log.i("ANSURGCM", "Unregistering device...");
		if (GCMRegistrar.isRegisteredOnServer(context)) {
			AppConnectionManager.unregister(context, registrationId);
		} else {
			// This callback results from the call to unregister made on
			// MainActivity when the registration to the server failed.
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