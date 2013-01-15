package com.example.streamwithvlc;

import java.io.IOException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

public class DownloadFileActivity extends Activity {
	public static final String DOWNLOAD_MESSAGE_ACTION = "Ansur.download_message_action";
	public static final String DMessage_ISDONE = "Ansur.download_message_isdone";
	public static final String DMessage_MOVIEPATH = "Ansur.download_message_moviepath";
	public static final String DMessage_FILESIZE = "Ansur.download_message_filesize";
	public static final String DMessage_BYTESDOWNLOADED = "Ansur.download_message_bytesdonwloaded";
	
	
	String movie_path = "";
	Button playMovie;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		registerReceiver(mHandleMessageReceiver,new IntentFilter(MainActivity.TOAST_MESSAGE_ACTION));
		registerReceiver(mHandleMessageReceiver2,new IntentFilter(DOWNLOAD_MESSAGE_ACTION));
		setContentView(R.layout.activity_download_file);
		
		//Get filePath
		Bundle extra = getIntent().getExtras();
		String file_path = extra.getString("FILE_PATH");
		
		//Disable the button, so that it can't be pressed before the movie is downloaded
		playMovie = (Button)findViewById(R.id.playMovieButton);
		playMovie.setEnabled(false);
		downloadFile(file_path);
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
		unregisterReceiver(mHandleMessageReceiver2);
		super.onDestroy();
	}
	
	protected void downloadFile(final String file_path) {
		AsyncTask<Void, Void, Void> deregisterServerTask = new AsyncTask<Void, Void, Void>() {
			
			@Override
			protected Void doInBackground(Void... params) {
				Context appContext = getApplicationContext();
				try {
					//Get movie name and endable button
					AppConnectionManager.downloadMotionRecord(appContext, file_path);
				} catch (UnknownHostException e) {
					DownloadFileActivity.this.maketoast(appContext, e.getMessage());
				} catch (IOException e) {
					DownloadFileActivity.this.maketoast(appContext, e.getMessage());
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
			maketoast(context, message);
		}
	};
	
	private final BroadcastReceiver mHandleMessageReceiver2 = new BroadcastReceiver() {
		private ProgressDialog dialog;

		@Override
		public void onReceive(Context context, Intent intent) {
			
			boolean isDone = intent.getExtras().getBoolean(DMessage_ISDONE);
			//check if the movie download is done
			if (isDone) {
				// download finished. set moviepath and the play button enabled
				movie_path=intent.getExtras().getString(DMessage_MOVIEPATH);
				DownloadFileActivity.this.playMovie.setEnabled(true);
				if (dialog != null) {
					dialog.dismiss();
				}
			} else {
				// not finished. Update the process bar
				int filesize = intent.getExtras().getInt(DMessage_FILESIZE);
				int bytesdownloaded = intent.getExtras().getInt(DMessage_BYTESDOWNLOADED);
				
				if (dialog == null) {
					dialog = new ProgressDialog(context);
					dialog.setCancelable(false);
			        dialog.setMessage("Downloading...");
			     // set the progress to be horizontal
			        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			        // display the progressbar
			        dialog.show();
				}
		        
		        // set the current status
		        dialog.setProgress(bytesdownloaded);
		     
		        // set the maximum value
		        dialog.setMax(filesize);
		       
			}
		}
	};
	
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.playMovieButton:
			playMovie();
			default: //unkown button pressed ;-)
		}
	}
	
	public void playMovie(){
		MediaController mc;
		VideoView videoView = (VideoView) findViewById(R.id.motionVideoView);
		videoView.setVideoPath(movie_path);
		mc = new MediaController(this);
		videoView.setMediaController(mc);
		videoView.start();
	}

	private void maketoast(final Context context, final String message) {
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
			}
		});
		
	}
	
}
