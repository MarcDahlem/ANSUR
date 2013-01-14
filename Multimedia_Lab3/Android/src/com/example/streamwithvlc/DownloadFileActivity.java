package com.example.streamwithvlc;

import java.io.IOException;
import java.net.UnknownHostException;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;

public class DownloadFileActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
	
	protected void downloadFile() {
		AsyncTask<Void, Void, Void> deregisterServerTask = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				try {
					ConnectionManager.downloadMotionRecord(getApplicationContext(), "/home/marc/Arbeitsfl�che/2012_01_11_hospital_invoice_krankenkasse");
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

}