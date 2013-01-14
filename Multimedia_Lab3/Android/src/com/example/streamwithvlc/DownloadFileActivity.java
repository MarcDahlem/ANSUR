package com.example.streamwithvlc;

import java.io.IOException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Toast;

public class DownloadFileActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		registerReceiver(mHandleMessageReceiver,new IntentFilter(MainActivity.TOAST_MESSAGE_ACTION));
		setContentView(R.layout.activity_download_file);
		downloadFile();
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_download_file, menu);
		return true;
	}
	
	@Override
	protected void onDestroy() {
		// destroy 
		unregisterReceiver(mHandleMessageReceiver);
		super.onDestroy();
	}
	
	protected void downloadFile() {
		AsyncTask<Void, Void, Void> deregisterServerTask = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				try {
					AppConnectionManager.downloadMotionRecord(getApplicationContext(), "/home/marc/Arbeitsfl�che/2012_01_11_hospital_invoice_krankenkasse");
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
			}

		};
		deregisterServerTask.execute(null,null,null);
	}
	
	private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String message = intent.getExtras().getString(MainActivity.EXTRA_MESSAGE);
			Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
		}
	};

}
