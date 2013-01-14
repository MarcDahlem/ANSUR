package com.example.streamwithvlc;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.Toast;
import classes.Room;

import com.example.streamwithvlc.helper.RoomListviewAdapter;

public class ListCamerasActivity extends Activity {

	private AsyncTask<Void, Void, Void> refreshTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cameralist);

		registerReceiver(mHandleMessageReceiver,new IntentFilter(MainActivity.TOAST_MESSAGE_ACTION));

		// Get the ExpandableListView from the layout

		ExpandableListView listView = getListView();

		//getExpandableListView().setGroupIndicator(null);
		//getExpandableListView().setDivider(null);
		//getExpandableListView().setDividerHeight(0);
		//registerForContextMenu(getExpandableListView());

		Collection<Room> rooms = this.generateRooms();

		updateListview(listView, rooms);
		this.refreshList();

	}


	private ExpandableListView getListView() {
		ExpandableListView listView = (ExpandableListView) findViewById(R.id.listView);
		return listView;
	}


	private void refreshList() {
		final Context context = this;
		refreshTask = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {

				try {
					final Collection<Room> newRooms = ConnectionManager.getAllCameras(context);
					final ExpandableListView listView = ListCamerasActivity.this.getListView();
					ListCamerasActivity.this.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							ListCamerasActivity.this.updateListview(listView, newRooms);
						}
					});
				} catch (final IOException e) {
					Log.e("ANSUR", "Get cameras error: ", e);
					ListCamerasActivity.this.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
						}
					});
				}

				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				refreshTask = null;
			}

		};
		refreshTask.execute(null, null, null);
	}


	private void updateListview(ExpandableListView listView,
			Collection<Room> rooms) {
		if (listView.getExpandableListAdapter() == null) {
			RoomListviewAdapter adapter = new RoomListviewAdapter(this,rooms);
			listView.setAdapter(adapter);
		} else {
			RoomListviewAdapter adapter = (RoomListviewAdapter)listView.getExpandableListAdapter();
			adapter.setRooms(rooms);
			adapter.notifyDataSetChanged();
		}
	}


	public void onClick(View view) {
		switch(view.getId()) {

		//Move the user to "new movie" page
		case R.id.subscribeButton:
			Toast.makeText(getApplicationContext(), "Subscribing", Toast.LENGTH_SHORT).show();
			break;

		case R.id.refreshButton:
			Toast.makeText(getApplicationContext(), "Refreshing", Toast.LENGTH_SHORT).show();
			break;

		case R.id.list_back_Button:
			finish();
			break;
		}
	}

	private Collection<Room> generateRooms() {		
		Map<String, Room> knownRooms = new TreeMap<String,Room>();
		return knownRooms.values();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(mHandleMessageReceiver);

		if (refreshTask != null) {
			refreshTask.cancel(true);
		}
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
