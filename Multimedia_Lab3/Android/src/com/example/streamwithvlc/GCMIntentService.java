package com.example.streamwithvlc;

import android.content.Context;
import android.content.Intent;

//import com.google.android.gcm.GCMBaseIntentService;

public class GCMIntentService/* extends GCMBaseIntentService*/ {

	/*public GCMIntentService(String... senderIDs){
		super(senderIDs);
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

	}

	@Override
	protected void onUnregistered(Context context, String registrationId) {
		// TODO send id to server in order to unregister the device from the server

	}
	
	@Override
	protected boolean onRecoverableError(Context context, String errorId) {*/
		/** Called when the device tries to register or unregister, but the GCM servers are unavailable.
		 * The GCM library will retry the operation using exponential backup, unless this method is 
		 * overridden and returns false.
		 * This method is optional and should be overridden only if you want to display the message to
		 * the user or cancel the retry attempts.
		 */
		/*return super.onRecoverableError(context, errorId);
	}*/
}