package connectionManager;

import java.util.EventListener;

public interface ConnectionListener extends EventListener{
	
	void eventAppeared(ConnectionEvent event);

}
