package com.example.streamwithvlc;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

public class GCMIntentService extends GCMBaseIntentService {

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