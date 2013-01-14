package com.example.streamwithvlc;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
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
		// TODO Auto-generated method stub
		Bundle extras = intent.getExtras(); 
		String msg = extras.getString("COMMAND");

		String content;
		switch(GcmMessages.GCMCOMMAND.valueOf(msg)){
		case MOTION_START:
			content = extras.getString("FILE_PATH");
			createNotificationMotionDetected("A motion is detected", "There has been detected a motion on camera 1", content.hashCode());
			break;
			
		case MOTION_END:
			content = extras.getString("FILE_PATH");
			createNotificationMotionStopped("Motion has stopped", "Motion that was detected can be downloaded here", content.hashCode());
			break;
			
		case CAMERA_DOWN:
			content = extras.getString("PORT");
			createNotificationCameraStopped("Lost camera", "Camera on port: " + content + " was disconnected", content.hashCode());
			break;
			
		case SERVER_SHUTDOWN:
			createNotificationServerStopped("Server down", "The server was shut down",1);
			break;
		}

	}

	
	public void createNotificationMotionDetected(String title, String message, int notificationID) {
		NotificationCompat.Builder mBuilder =
		        new NotificationCompat.Builder(this)
		        .setSmallIcon(R.drawable.motion_detected)
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
		stackBuilder.addParentStack(MainActivity.class);
		
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(launchApplication());
		PendingIntent resultPendingIntent =
		        stackBuilder.getPendingIntent(
		            0,
		            PendingIntent.FLAG_UPDATE_CURRENT
		        );
		mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager =
		    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		// notificationID allows you to update the notification later on.
		mNotificationManager.notify(notificationID, mBuilder.build());
		
	}
	
	public void createNotificationMotionStopped(String title, String message,int notificationID ) {
		//TODO: Same as before, only that instead if going to MainActivity, you download movie through connectionmanager
		
	}
	
	public void createNotificationCameraStopped(String title, String message,int notificationID ) {
		//TODO: Same as before, only that instead if going to MainActivity, you download movie through connectionmanager
		
	}
	
	public void createNotificationServerStopped(String title, String message,int notificationID ) {
		//TODO: Same as before, only that instead if going to MainActivity, you download movie through connectionmanager
		
	}
	
	public Intent launchApplication(){
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
		// TODO Typically, there is nothing to be done other than evaluating the error (returned by errorId) and trying to fix the problem.

	}

	@Override
	protected void onRegistered(Context context, String registrationId) {
		ConnectionManager.register(context, registrationId);
	}

	@Override
	protected void onUnregistered(Context context, String registrationId) {
		// TODO send id to server in order to unregister the device from the server
		Log.i("ANSURGCM", "Unregistering device...");
		if (GCMRegistrar.isRegisteredOnServer(context)) {
			ConnectionManager.unregister(context, registrationId);
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