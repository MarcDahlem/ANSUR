package com.example.streamwithvlc;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.google.android.gcm.GCMRegistrar;

public class MainActivity extends Activity {

	public static final String SENDER_ID = "862106151827";
	//public static String HOSTNAME = "192.168.37.36";
	//public static String HOSTNAME = "130.240.93.97";
	public static String HOSTNAME = "130.240.93.107";
	public static int PORT = 5000;
	private AsyncTask<Void, Void, Void> registerTask;
	/**
	 * Intent used to toast a message
	 */
	public static final String TOAST_MESSAGE_ACTION = "com.example.ansur.TOAST_MESSAGE";

	/**
	 * Intent's extra that contains the message to be displayed.
	 */
	static final String EXTRA_MESSAGE = "message";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		registerReceiver(mHandleMessageReceiver,new IntentFilter(TOAST_MESSAGE_ACTION));

		GCMRegistrar.checkDevice(getApplicationContext());
		GCMRegistrar.checkManifest(getApplicationContext());
	}

	@Override
	public void onAttachedToWindow() {
		//fix the background
		super.onAttachedToWindow();
		Window window = getWindow();
		window.setFormat(PixelFormat.RGBA_8888);
	}


	public void registerDevice() {
		final String regId = GCMRegistrar.getRegistrationId(getApplicationContext());
		if (regId.equals("")) {
			//not registered yet. Automatically register on startup
			GCMRegistrar.register(getApplicationContext(), SENDER_ID);
		} else {
			// Device is already registered on GCM, check server.
			Log.v("GCM", "Device Already registered on GCM, try to register on the server.");
			Toast.makeText(getApplicationContext(), "Device Already registered on GCM, try to register on the server.",
					Toast.LENGTH_SHORT).show();
			if (GCMRegistrar.isRegisteredOnServer(getApplicationContext())) {
				// Skips registration.
				Toast.makeText(getApplicationContext(), "Device is registered on server",
						Toast.LENGTH_SHORT).show();
			} else {
				// Try to register again, but not in the UI thread.
				// It's also necessary to cancel the thread onDestroy(),
				// hence the use of AsyncTask instead of a raw thread.
				final Context context = getApplicationContext();
				registerTask = new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... params) {
						boolean registered = AppConnectionManager.register(context, regId);
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


	public void deregisterDevice() {
		final String regId = GCMRegistrar.getRegistrationId(getApplicationContext());
		if (regId.equals("")) {
			//not registered yet. nothing to do
			return;
		} else {
			// Device is registered on GCM, check if it is also registered on server
			final Context context = getApplicationContext();
			if (GCMRegistrar.isRegisteredOnServer(getApplicationContext())) {
				// deregister from server first

				AsyncTask<Void, Void, Void> deregisterServerTask = new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... params) {
						AppConnectionManager.unregister(context, regId);
						return null;
					}

					@Override
					protected void onPostExecute(Void result) {
					}

				};
				deregisterServerTask.execute(null, null, null);
			}

			// only registered on GCM here. Unregister from gcm. (if deregistration from server failed so will the server get an invalid member the next time he tries to send a message)
			AsyncTask<Void, Void, Void> deregisterTask = new AsyncTask<Void, Void, Void>(){

				@Override
				protected Void doInBackground(Void... params) {
					GCMRegistrar.unregister(context);
					return null;
				}

				@Override
				protected void onPostExecute(Void result) {
				}

			};
			deregisterTask.execute(null, null, null);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// Currently no menu
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	/**
	 * Choices for the menu
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.settings:
			startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onClick(View view) {
		switch(view.getId()) {

		//Move the user to "new movie" page
		case R.id.startButton:
			//launchApplication();
			if (!GCMRegistrar.isRegistered(getApplicationContext())) {
				Toast.makeText(getApplicationContext(), "Device not registered on GCM. Please register first.",Toast.LENGTH_SHORT).show();
				break;
			}

			if (!GCMRegistrar.isRegisteredOnServer(getApplicationContext())) {
				Toast.makeText(getApplicationContext(), "GCM registered, but not on the server. Please try to register again.",Toast.LENGTH_SHORT).show();
				break;
			}
			//everything is registered, can step to the next activity
			startActivity(new Intent(view.getContext(), ListCamerasActivity.class));
			break;

		case R.id.connectButton:
			registerDevice();
			break;

		case R.id.dConnectButton:
			deregisterDevice();
			break;
		}
	}
	
	@Override
	protected void onDestroy() {
		// destroy 
		unregisterReceiver(mHandleMessageReceiver);

		if (registerTask != null) {
			registerTask.cancel(true);
		}
		GCMRegistrar.onDestroy(getApplicationContext());
		super.onDestroy();
	}

	private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String message = intent.getExtras().getString(EXTRA_MESSAGE);
			Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
		}
	};
	
    /**
     * Notifies UI to toast a message.
     *
     * @param context application's context.
     * @param message message to be toasted.
     */
    public static void displayMessage(Context context, String message) {
        Intent intent = new Intent(TOAST_MESSAGE_ACTION);
        intent.putExtra(EXTRA_MESSAGE, message);
        context.sendBroadcast(intent);
    }

}
