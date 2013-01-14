package classes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import junit.framework.Assert;

public class Room {
	
	public static final String SUBSCRIPTION = "ROOM_OUTSTANDING_SUBSCRIPTIONS";
	public static final String UNSUBSCRIPTION = "ROOM_OUTSTANDING_UNSUBSCRIPTIONS";
	
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

	/**
	 * 
	 * @return true if all cameras of this room are selected
	 */
	public boolean isSelected() {
		// returns if the room is checked or not
		
		for (Camera cam: cams) {
			if (!cam.isSelected()) {
				return false;
			}
		}
		//all cameras are selected
		return true;
	}

	/**If the room needs to be updated
	 * 
	 * @return boolean needsupdate if at minimum one of the cameras in this room needs to be updated
	 */
	public boolean needsUpdate() {
		for (Camera cam:this.cams) {
			if (cam.needsUpdate()) {
				return true;
			}
		}
		
		//no camera needs to be updated
		return false;
	}

	/** Get all updates that are available in this room. Returns empty lists if there exists no update (but Map contains always two lists!)
	 * 
	 * @return Map<String, List<Camera>> a map with {@link #Room.SUBSCRIPTION} for all cameras that needs a subscribe update
	 * 			and {@link #Room.UNSUBSCRIPTION} for all cameras that needs to be unsubscribed
	 */
	public Map<String, List<Camera>> getOutstandingUpdates() {
		TreeMap<String, List<Camera>> result = new TreeMap<String, List<Camera>>();
		List<Camera> outSub = new ArrayList<Camera>();
		List<Camera> outUnsub = new ArrayList<Camera>();
		
		for (Camera cam: this.cams) {
			if (cam.needsUpdate()) {
				if (cam.isOutstandingForSubscription()) {
					outSub.add(cam);
				} else {
					Assert.assertTrue(cam.isOutstandingForUnsubscription());
					outUnsub.add(cam);
				}
			}
		}
		
		result.put(Room.SUBSCRIPTION, outSub);
		result.put(Room.UNSUBSCRIPTION, outUnsub);
		return result;
	}
}
