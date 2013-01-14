package classes;

import java.util.ArrayList;

public class Room {
	
	private String name;
	private ArrayList<Camera> cams;
	private boolean isSelected;

	public Room(String roomName) {
		this.name = roomName;
		this.cams = new ArrayList<Camera>();
	}
	
	public String getRoomname () {
		return this.name;
	}

	/**
	 * @return the cams
	 */
	public ArrayList<Camera> getCameras() {
		return cams;
	}

	public void addCamera(Camera cam) {
		this.cams.add(cam);
	}
	
	public void setSelection(boolean isSelected) {
		this.isSelected = isSelected;
	}

	public boolean isSelected() {
		// returns if the room is checked or not
		return this.isSelected;
	}
}
