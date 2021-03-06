package com.example.streamwithvlc.helper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import classes.Camera;
import classes.Room;

import com.example.streamwithvlc.R;

public class RoomListviewAdapter extends BaseExpandableListAdapter{
	private LayoutInflater inflater;
	private Room[] rooms;
	private Context context;

	public RoomListviewAdapter(Context context, Collection<Room> rooms) {
		super();
		this.inflater = LayoutInflater.from(context);
		this.setRooms(rooms);
		this.context=context;
	}

	@Override
	public Camera getChild(int groupPosition, int childPosition) {
		return rooms[groupPosition].getCameras().get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return rooms[groupPosition].getCameras().get(childPosition).getPort();
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		Room room = rooms[groupPosition];
		Camera cam = room.getCameras().get(childPosition);
		convertView = inflater.inflate(R.layout.camrow, parent, false);
		((TextView) convertView.findViewById(R.id.camname)).setText(cam.toString());
		CheckBox checkbox = (CheckBox) convertView.findViewById(R.id.camCheckbox);
		boolean selected = cam.isSelected();
		
		checkbox.setChecked(selected);
		
		if (cam.needsUpdate()) {
			convertView.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.bg_orange));
		}
		
		checkbox.setOnCheckedChangeListener(new CamClickListener(cam, this));
		return convertView;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return rooms[groupPosition].getCameras().size();
	}

	@Override
	public Room getGroup(int groupPosition) {
		return rooms[groupPosition];
	}

	@Override
	public int getGroupCount() {
		return rooms.length;
	}

	@Override
	public long getGroupId(int groupPosition) {
		return this.getGroup(groupPosition).getRoomname().hashCode();
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		Room room = rooms[groupPosition];
		convertView = inflater.inflate(R.layout.roomrow, parent, false);
		((TextView) convertView.findViewById(R.id.roomName)).setText(room.getRoomname());
		CheckBox checkbox = (CheckBox) convertView.findViewById(R.id.roomCheckbox);
		boolean selected = room.isSelected();
		checkbox.setChecked(selected);
		
		if (room.needsUpdate()) {
			convertView.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.bg_orange));
		}
		checkbox.setOnCheckedChangeListener(new RoomClickListener(room, this));
		return convertView;	
	}

	@Override
	public boolean hasStableIds() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		// TODO Auto-generated method stub
		return true;
	}

	public void setRooms(Collection<Room> rooms) {
		for (Room room:rooms) {
			Collections.sort(room.getCameras());
		}
		this.rooms=rooms.toArray(new Room[0]);
	}

	public Collection<Room> getRooms() {
		return Arrays.asList(this.rooms);
	}

	public void removeCamera(String cameraName, String roomName, int cameraPort) {
		for (Room room: this.rooms) {
			if (room.getRoomname().equals(roomName)) {
				for (Camera cam:room.getCameras()) {
					if (cam.getPort() == cameraPort && cam.getName().equals(cameraName)) {
						room.removeCamera(cam);
						this.notifyDataSetChanged();
						return;
					}
				}
			}
		}
	}

}

