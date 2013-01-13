package com.example.streamwithvlc;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import com.google.android.gcm.GCMRegistrar;

public class MainActivity extends Activity {

	public static final String SENDER_ID = "862106151827";
	public static final String HOSTNAME = "130.240.93.97";
	public static final int PORT = 5000;
	private AsyncTask<Void, Void, Void> registerTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		GCMRegistrar.checkDevice(this);
		GCMRegistrar.checkManifest(this);
	}


	public void registerDevice() {
		final String regId = GCMRegistrar.getRegistrationId(this);
		if (regId.equals("")) {
			//not registered yet. Automatically register on startup
			GCMRegistrar.register(this, SENDER_ID);
		} else {
			// Device is already registered on GCM, check server.
			Log.v("GCM", "Device Already registered on GCM, try to register on the server.");
			Toast.makeText(getApplicationContext(), "Device Already registered on GCM, try to register on the server.",
					Toast.LENGTH_SHORT).show();
			if (GCMRegistrar.isRegisteredOnServer(this)) {
				// Skips registration.
				Toast.makeText(getApplicationContext(), "Device is registerd on server",
						Toast.LENGTH_SHORT).show();
			} else {
				// Try to register again, but not in the UI thread.
				// It's also necessary to cancel the thread onDestroy(),
				// hence the use of AsyncTask instead of a raw thread.
				final Context context = this;
				registerTask = new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... params) {
						boolean registered = ConnectionManager.register(context, regId);
						// At this point all attempts to register with the app
						// server failed, so we need to unregister the device
						// from GCM - the app will try to register again when
						// it is restarted. Note that GCM will send an
						// unregistered callback upon completion, but
						// GCMIntentService.onUnregistered() will ignore it.
						if (!registered) {
							GCMRegistrar.unregister(context);
						}
							
						return null;
					}

					@Override
					protected void onPostExecute(Void result) {
						registerTask = null;
					}

				};
				registerTask.execute(null, null, null);
			}

		}
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// Currently no menu
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public void onClick(View view) {
		switch(view.getId()) {

		//Move the user to "new movie" page
		case R.id.startButton:
			//launchApplication();
			startActivity(new Intent(view.getContext(), ListCamerasActivity.class));
			break;
		
		case R.id.connectButton:
			registerDevice();
			break;
			
		case R.id.dConnectButton:
			break;
		}
	}

	public void launchApplication(){
		PackageManager packageManager = getApplicationContext().getPackageManager();
		Intent applicationIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);

		// Verify clock implementation
		// Use package explorer in google play store to find package name and activity name
		String clockImpls[][] = {
				{"VLC Player", "org.videolan.vlc.android", "org.videolan.vlc.android.MainActivity"},
				//{"HTC Alarm Clock", "com.htc.android.worldclock", "com.htc.android.worldclock.WorldClockTabControl" },
				{"Standard Alarm Clock", "com.android.deskclock", "com.android.deskclock.AlarmClock"},
				{"Froyo Nexus Alarm Clock", "com.google.android.deskclock", "com.android.deskclock.DeskClock"},
				{"Moto Blur Alarm Clock", "com.motorola.blur.alarmclock",  "com.motorola.blur.alarmclock.AlarmClock"},
				{"Samsung Galaxy Clock", "com.sec.android.app.clockpackage","com.sec.android.app.clockpackage.ClockPackage"}
		};

		boolean foundClockImpl = false;

		for(int i=0; i<clockImpls.length; i++) {
			String vendor = clockImpls[i][0];
			String packageName = clockImpls[i][1];
			String className = clockImpls[i][2];
			try {
				ComponentName cn = new ComponentName(packageName, className);
				ActivityInfo aInfo = packageManager.getActivityInfo(cn, PackageManager.GET_META_DATA);
				applicationIntent.setComponent(cn);
				Log.d("Debug", "Found " + vendor + " --> " + packageName + "/" + className);
				foundClockImpl = true;
			} catch (NameNotFoundException e) {
				Log.d("Debug", vendor + " does not exists");
			}
		}

		if (foundClockImpl) {
			applicationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(applicationIntent);
		}
	}

	@Override
	protected void onDestroy() {
		// destroy 
		if (registerTask != null) {
			registerTask.cancel(true);
		}
		GCMRegistrar.onDestroy(this);
		super.onDestroy();
	}

}
