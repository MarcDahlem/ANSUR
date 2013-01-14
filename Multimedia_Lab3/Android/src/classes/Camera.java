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

	public void setSubscribed(boolean isSubscribed) {
		this.isSubscribed=isSubscribed;
	}

	/** Checks if the camera needs to be updated.
	 * More detailed information can be collected over {@link #isOutstandingForSubscription()} and {@link #isOutstandingForUnsubscription()}
	 * 
	 * If and only if it returns true it is guaranteed, that exactly one out of {@link #isOutstandingForSubscription()} and {@link #isOutstandingForUnsubscription()} returns true.
	 * 
	 * @return boolean returns true if the selection status differs from the subscription status
	 */
	public boolean needsUpdate() {
		return this.isSelected != this.isSubscribed;
	}

	/** Checks if this camera needs to be subscribed.
	 * If this returns true the method {@link #isOutstandingForUnsubscription()} cannot return true.
	 * @return true if this cam is not subscribed yet but selected, false otherwise
	 */
	public boolean isOutstandingForSubscription() {
		return (!this.isSubscribed) && this.isSelected;
	}

	/** Checks if this camera needs to be unsubscribed.
	 * If this method return true, the method {@link #isOutstandingForSubscription()} cannot return true
	 * 
	 * @return true if this camera is subscribed but no more selected, false otherwise
	 */
	public boolean isOutstandingForUnsubscription() {
		return this.isSubscribed && (!this.isSelected);
	}
	
}
