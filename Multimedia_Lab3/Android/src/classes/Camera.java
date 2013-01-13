package classes;

public class Camera {

	String name;
	int port;
	boolean isSelected = false;
	
	public Camera(String name, int port, boolean isSelected){
		this.name = name;
		this.port = port;
		this.isSelected = isSelected;
	}

	public String getName() {
		return name;
	}

	public int getPort() {
		return port;
	}
	
		public boolean isSelected() {
		return isSelected;
	}

	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}

	public String toString(){
		return new String("Camera: " + name + " Port: " + port);
	}
	
}
