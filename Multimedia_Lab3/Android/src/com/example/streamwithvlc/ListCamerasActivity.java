package com.example.streamwithvlc;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ExpandableListView;
import classes.Camera;
import classes.Room;

import com.example.streamwithvlc.helper.RoomListviewAdapter;

public class ListCamerasActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_cameralist);

        // Get the ExpandableListView from the layout
        ExpandableListView listView = (ExpandableListView) findViewById(R.id.listView);

		//getExpandableListView().setGroupIndicator(null);
		//getExpandableListView().setDivider(null);
		//getExpandableListView().setDividerHeight(0);
		//registerForContextMenu(getExpandableListView());
		
		Collection<Room> rooms = this.generateRooms();
		
		if (listView.getExpandableListAdapter() == null) {
			RoomListviewAdapter adapter = new RoomListviewAdapter(this,rooms);
			listView.setAdapter(adapter);
		} else {
			RoomListviewAdapter adapter = (RoomListviewAdapter)listView.getExpandableListAdapter();
			adapter.setRooms(rooms);
			adapter.notifyDataSetChanged();
		}
	}

	private Collection<Room> generateRooms() {
		String room1Name = "ZYX";
		String room2Name = "ABC";
		
		Room room1 = new Room(room1Name);
		Room room2 = new Room (room2Name);
		
		Map<String, Room> knownRooms = new TreeMap<String,Room>();
		knownRooms.put(room1Name, room1);
		knownRooms.put(room2Name, room2);
		
		Camera cam1 = new Camera("zyx", 8000, false);
		Camera cam2 = new Camera("abc", 8001, false);
		Camera cam3 = new Camera("wvu",8002,false);
		Camera cam4 = new Camera("def", 8003, false);
		
		Room room = knownRooms.get(room2Name);
		room.addCamera(cam3);
		room.addCamera(cam4);
		
		room = knownRooms.get(room1Name);
		room.addCamera(cam1);
		room.addCamera(cam2);
		return knownRooms.values();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

}
