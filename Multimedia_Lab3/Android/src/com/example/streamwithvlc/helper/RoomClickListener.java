package com.example.streamwithvlc.helper;

import classes.Camera;
import classes.Room;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class RoomClickListener implements OnCheckedChangeListener {

	private RoomListviewAdapter parent;
	private Room room;

	public RoomClickListener(Room room, RoomListviewAdapter parent) {
		this.room = room;
		this.parent=parent;
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		room.setSelection(isChecked);
		for(Camera camera:room.getCameras()) {
			camera.setSelection(isChecked);
		}
		parent.notifyDataSetChanged();
	}

}
