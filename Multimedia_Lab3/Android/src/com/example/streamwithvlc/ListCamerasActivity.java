package com.example.streamwithvlc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
import classes.Camera;
import classes.Room;

import com.example.streamwithvlc.helper.RoomListviewAdapter;
import com.google.android.gcm.GCMRegistrar;

public class ListCamerasActivity extends Activity {

	public static final String CAMERA_DISC_ACTION = "com.example.ansur.CAMERA_DISC_ACTION";
	public static final String CAM_DISC_ROOM = "CAM_DISC_ROOM";
	public static final String CAM_DISC_CAM = "CAM_DISC_CAM";
	public static final String CAM_DISC_PORT = "CAM_DISC_PORT";
	
	private AsyncTask<Void, Void, Void> refreshTask;
	private AsyncTask<Void, Void, Void> subscribeTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cameralist);

		registerReceiver(mHandleMessageReceiver,new IntentFilter(MainActivity.TOAST_MESSAGE_ACTION));
		registerReceiver(mHandleMessageReceiver2,new IntentFilter(ListCamerasActivity.CAMERA_DISC_ACTION));
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
		final Context context = this.getApplicationContext();
		refreshTask = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {

				try {
					String regId =GCMRegistrar.getRegistrationId(context);
					final Collection<Room> newRooms = AppConnectionManager.getAllCameras(context, regId);
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
			RoomListviewAdapter adapter = new RoomListviewAdapter(this.getApplicationContext(),rooms);
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
			Toast.makeText(getApplicationContext(), "Update subscriptions", Toast.LENGTH_SHORT).show();
			this.updateSubscriptions();
			break;

		case R.id.refreshButton:
			Toast.makeText(getApplicationContext(), "Refreshing", Toast.LENGTH_SHORT).show();
			this.refreshList();
			break;

		case R.id.list_back_Button:
			finish();
			break;
		}
	}

	private void updateSubscriptions() {
		final Context context = this.getApplicationContext();
		subscribeTask = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {

				try {
					String regId =GCMRegistrar.getRegistrationId(context);

					ExpandableListView listView = ListCamerasActivity.this.getListView();
					final RoomListviewAdapter adapter = (RoomListviewAdapter)listView.getExpandableListAdapter();

					Collection<Room> rooms = adapter.getRooms();
					ArrayList<Camera> sub_cameras=new ArrayList<Camera>();
					ArrayList<Camera> unsub_cameras=new ArrayList<Camera>();

					for (Room room:rooms) {
						if (room.needsUpdate()) {
							Map<String, List<Camera>> outstandings = room.getOutstandingUpdates();
							List<Camera> outSub = outstandings.get(Room.SUBSCRIPTION);
							List<Camera> outUnsub = outstandings.get(Room.UNSUBSCRIPTION);
							sub_cameras.addAll(outSub);
							unsub_cameras.addAll(outUnsub);
						}
					}

					if (!sub_cameras.isEmpty()) {
						AppConnectionManager.subscribeTo(context, sub_cameras, regId);


						for (Camera sub_camera:sub_cameras) {
							sub_camera.setSubscribed(true);
						}
					}

					if (!unsub_cameras.isEmpty()) {
						AppConnectionManager.unsubscribeFrom(context, unsub_cameras, regId);

						for (Camera unsub_cam : unsub_cameras) {
							unsub_cam.setSubscribed(false);
						}
					}

					ListCamerasActivity.this.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							adapter.notifyDataSetChanged();
						}
					});
				} catch (final IOException e) {
					Log.e("ANSUR", "(Un)Subscribe error: ", e);
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
				subscribeTask = null;
			}

		};
		subscribeTask.execute(null, null, null);
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
		unregisterReceiver(mHandleMessageReceiver2);

		if (refreshTask != null) {
			refreshTask.cancel(true);
		}

		if (subscribeTask!=null) {
			subscribeTask.cancel(true);
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
	private final BroadcastReceiver mHandleMessageReceiver2 = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String cameraName = intent.getExtras().getString(ListCamerasActivity.CAM_DISC_CAM);
			String roomName = intent.getExtras().getString(ListCamerasActivity.CAM_DISC_ROOM);
			int cameraPort = Integer.parseInt(intent.getExtras().getString(ListCamerasActivity.CAM_DISC_PORT));
			Toast.makeText(context, "Camera " + cameraName + " in room " + roomName + " on port " + cameraPort + "disconnected.", Toast.LENGTH_SHORT).show();
			
			ListCamerasActivity.this.removeCamera(cameraName, roomName, cameraPort);
		}
	};

	protected void removeCamera(String cameraName, String roomName, int cameraPort) {
		// camera disconnected
		ExpandableListView listView = this.getListView();
		RoomListviewAdapter adapter = (RoomListviewAdapter)listView.getExpandableListAdapter();
		adapter.removeCamera(cameraName, roomName, cameraPort);
	}
}
