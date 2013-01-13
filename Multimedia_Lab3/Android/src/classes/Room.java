package classes;

import java.util.ArrayList;

public class Room {
	
	private String name;
	private ArrayList<Camera> cams;

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
}
