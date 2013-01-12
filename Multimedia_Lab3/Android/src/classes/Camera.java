package classes;

public class Camera {

	String name = "", room = "";
	int port;
	boolean isSelected = false;
	
	public Camera(String name, String room, int port, boolean isSelected){
		this.name = name;
		this.room = room;
		this.port = port;
		this.isSelected = isSelected;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRoom() {
		return room;
	}

	public void setRoom(String room) {
		this.room = room;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	
	
	public boolean isSelected() {
		return isSelected;
	}

	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}

	public String toString(){
		return new String("Camera: " + name + "\n" + "Room: " + room + "\n" + "Port: " + port);
	}
	
}
