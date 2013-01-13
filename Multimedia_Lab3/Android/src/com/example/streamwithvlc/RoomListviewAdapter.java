package com.example.streamwithvlc;

import java.util.ArrayList;
import java.util.Collection;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import classes.Camera;
import classes.Room;

public class RoomListviewAdapter extends ArrayAdapter<Room>{
	private ArrayList<Room> roomList;

	public RoomListviewAdapter(Context context, int textViewResourceId, Collection<Room> rooms) {
		super(context, textViewResourceId, rooms.toArray(new Room[0]));
		this.roomList = new ArrayList<Room>();
		this.roomList.addAll(rooms);
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
			LayoutInflater vi = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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

		Room room = roomList.get(position);
		
		for(Camera c : room.getCameras()){
			holder.port.setText(" (" + c.getPort() + ")");
			holder.name.setText(room.getRoomname() + " - " + c.getName());
			holder.name.setChecked(c.isSelected());
			holder.name.setTag(c);
		}	
		return convertView;

	}

}

