package com.example.streamwithvlc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import classes.Camera;
import classes.Room;

/**
 * http://www.mysamplecode.com/2012/07/android-listview-checkbox-example.html
 * @author Jon Martin Mikalsen
 *
 */

public class ListCamerasActivity extends Activity {

	
	RoomListviewAdapter aa = null;
	ListView camerasListView;
	private Collection<Room> rooms;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list_cameras);
		setListView();
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_list_cameras, menu);
		return true;
	}

	public void setListView(){
		
		// set resID to be a specefic layout
		int resID = R.layout.camera_info;
		// Combine the list to the layout
		aa = new RoomListviewAdapter(this, resID, rooms);		
		// Gets the listview
		camerasListView = (ListView) findViewById(R.id.camerasListView);
		// Sets the adapter into the view
		camerasListView.setAdapter(aa);

	}
	
	
	public void onClick(View view) {

		Toast toast;
		switch (view.getId()) {
		case R.id.subscribe_button:
			checkIfSubscribed();
			break;

		case R.id.refresh_button:
			Toast.makeText(getApplicationContext(), "Refreshing list",
					Toast.LENGTH_SHORT).show();
			try {
				this.rooms = ConnectionManager.getAllCameras(this);
			} catch (IOException e) {
				Toast.makeText(this, "get all cameras failed", Toast.LENGTH_SHORT).show();
				Log.e("ANSUR", "get all cameras had an error:", e);
			}
			break;

		case R.id.cameras_back_button:
			finish();
			break;
		}
	}
	
	public void checkIfSubscribed() {
		StringBuffer responseText = new StringBuffer();
		responseText.append("The following were selected...");

		ArrayList<Camera> cameraList = null; //aa.getAllCameras();
		for (int i = 0; i < cameraList.size(); i++) {
			Camera camera = cameraList.get(i);
			if (camera.isSelected()) {
				responseText.append("\n" + camera.getName());
			}
		}

		Toast.makeText(getApplicationContext(), responseText, Toast.LENGTH_LONG)
				.show();
	}

}
