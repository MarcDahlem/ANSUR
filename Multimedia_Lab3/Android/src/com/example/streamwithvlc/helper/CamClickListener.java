package com.example.streamwithvlc.helper;

import classes.Camera;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class CamClickListener implements OnCheckedChangeListener {

	private Camera cam;
	private RoomListviewAdapter parent;

	public CamClickListener(Camera cam, RoomListviewAdapter parent) {
		this.cam=cam;
		this.parent=parent;
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		this.cam.setSelection(isChecked);
		parent.notifyDataSetChanged();
	}

}
