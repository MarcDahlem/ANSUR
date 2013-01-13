package com.example.streamwithvlc;

import java.util.ArrayList;

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

/**
 * http://www.mysamplecode.com/2012/07/android-listview-checkbox-example.html
 * @author Jon Martin Mikalsen
 *
 */

public class ListCamerasActivity extends Activity {

	
	MyCustomAdapter aa = null;
	ListView camerasListView;

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
		ArrayList<Camera> camerasList = new ArrayList<Camera>();
		camerasList.add(new Camera("Cam1", "Kitchen", 1000, false));
		camerasList.add(new Camera("Cam2", "Bedroom", 2000, false));
		
		// set resID to be a specefic layout
		int resID = R.layout.camera_info;
		// Combine the list to the layout
		aa = new MyCustomAdapter(this, resID, camerasList);		
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
			toast = Toast.makeText(getApplicationContext(), "Refreshing list",
					Toast.LENGTH_SHORT);
			toast.show();
			break;

		case R.id.cameras_back_button:
			finish();
			break;
		}
	}

	private class MyCustomAdapter extends ArrayAdapter<Camera> {
		private ArrayList<Camera> cameraList;

		public MyCustomAdapter(Context context, int textViewResourceId,
				ArrayList<Camera> cameraList) {
			super(context, textViewResourceId, cameraList);
			this.cameraList = new ArrayList<Camera>();
			this.cameraList.addAll(cameraList);
		}

		private class ViewHolder {
			TextView port;
			CheckBox name;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			ViewHolder holder = null;
			Log.v("ConvertView", String.valueOf(position));

			if (convertView == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = vi.inflate(R.layout.camera_info, null);

				holder = new ViewHolder();
				holder.port = (TextView) convertView.findViewById(R.id.port);
				holder.name = (CheckBox) convertView.findViewById(R.id.checkBox);
				convertView.setTag(holder);

				holder.name.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						CheckBox cb = (CheckBox) v;
						Camera camera = (Camera) cb.getTag();
						/*Toast.makeText(
								getApplicationContext(),
								"Clicked on Checkbox: " + cb.getText() + " is "
										+ cb.isChecked(), Toast.LENGTH_LONG)
								.show();*/
						camera.setSelected(cb.isChecked());
					}
				});
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			Camera camera = cameraList.get(position);
			holder.port.setText(" (" + camera.getPort() + ")");
			holder.name.setText(camera.getRoom() + " - " + camera.getName());
			holder.name.setChecked(camera.isSelected());
			holder.name.setTag(camera);

			return convertView;

		}

	}
	
	public void checkIfSubscribed() {
		StringBuffer responseText = new StringBuffer();
		responseText.append("The following were selected...");

		ArrayList<Camera> cameraList = aa.cameraList;
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
