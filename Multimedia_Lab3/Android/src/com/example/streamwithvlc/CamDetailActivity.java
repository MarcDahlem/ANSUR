package com.example.streamwithvlc;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toast;
import classes.Camera;

public class CamDetailActivity extends Activity {


	private Camera cam;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cam_detail);
		registerReceiver(mHandleMessageReceiver,new IntentFilter(MainActivity.TOAST_MESSAGE_ACTION));
		Bundle extras = getIntent().getExtras();
		this.cam = (Camera)extras.getSerializable("CLICKEDCAMERA");
	}

	@Override
	public void onAttachedToWindow() {
		//fix the background
		super.onAttachedToWindow();
		Window window = getWindow();
		window.setFormat(PixelFormat.RGBA_8888);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
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
		case R.id.launchVLCButton:
			launchApplication();
			break;

		case R.id.copyToClipbordButton:
			copyToClipbord();
			break;

		case R.id.camdetail_back_Button:
			finish();
			break;
		}
	}
	
	private void launchApplication() {
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
			startActivity(applicationIntent);
		}
	}

	private void copyToClipbord() {
		ClipboardManager clipboard = (ClipboardManager)
				getSystemService(Context.CLIPBOARD_SERVICE);
		ClipData clip = ClipData.newPlainText("Server","tcp://"+MainActivity.HOSTNAME+":"+(this.cam.getPort()+1));
		clipboard.setPrimaryClip(clip);
	}

	@Override
	protected void onDestroy() {
		// destroy 
		unregisterReceiver(mHandleMessageReceiver);
		super.onDestroy();
	}

	private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String message = intent.getExtras().getString(MainActivity.EXTRA_MESSAGE);
			Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
		}
	};
}
