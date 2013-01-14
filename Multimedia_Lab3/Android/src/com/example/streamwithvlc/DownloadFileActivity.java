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
import android.view.View;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

public class DownloadFileActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		registerReceiver(mHandleMessageReceiver,new IntentFilter(MainActivity.TOAST_MESSAGE_ACTION));
		setContentView(R.layout.activity_download_file);
		Bundle extra = getIntent().getExtras();
		downloadFile(extra.getString("FILE_PATH"));
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
	
	protected void downloadFile(final String file_path) {
		AsyncTask<Void, Void, Void> deregisterServerTask = new AsyncTask<Void, Void, Void>() {
			
			@Override
			protected Void doInBackground(Void... params) {
				try {
					AppConnectionManager.downloadMotionRecord(getApplicationContext(), file_path);
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
	
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.playMovieButton:
			playMovie();
		}
	}
	
	public void playMovie(){
		Bundle extras = getIntent().getExtras();
		
		String filePath = extras.getString("MOVIE_PATH");
		MediaController mc;
		VideoView videoView = (VideoView) findViewById(R.id.motionVideoView2);
		videoView.setVideoPath(filePath);
		mc = new MediaController(this);
		videoView.setMediaController(mc);
		videoView.start();
	}
	
}
