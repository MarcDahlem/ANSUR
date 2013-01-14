package classes;

public class Camera implements Comparable<Camera>{

	private String name;
	private int port;
	private boolean isSelected;
	private boolean isSubscribed;
	
	public Camera(String name, int port, boolean subscribed){
		this.name = name;
		this.port = port;
		this.isSelected = subscribed;
		this.isSubscribed = subscribed;
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

	public void setSelection(boolean isSelected) {
		this.isSelected = isSelected;
	}

	public String toString(){
		return new String("Camera: " + name + " Port: " + port);
	}

	public boolean isSubscribed() {
		return this.isSubscribed;
	}

	@Override
	public int compareTo(Camera another) {
		if (another==null) {
			return 1;
		} else {
			return this.toString().compareToIgnoreCase((another.toString()));
		}
	}
	
}
