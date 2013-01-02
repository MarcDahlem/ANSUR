package com.example.streamwithvlc;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
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
    		launchApplication();
    		break;
    		
    	case R.id.stopButton:
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

}