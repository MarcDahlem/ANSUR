package com.example.streamwithvlc;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.MediaController;
import android.widget.VideoView;

public class PlayMotionMovieActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_play_motion_movie);

		Bundle extras = getIntent().getExtras();
		
		String filePath = extras.getString("FILE_PATH");
		MediaController mc;
		VideoView videoView = (VideoView) findViewById(R.id.motionVideoView);
		videoView.setVideoPath(filePath);
		mc = new MediaController(this);
		videoView.setMediaController(mc);
		videoView.start();
	}
	
	
	

}
