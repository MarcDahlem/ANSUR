package client.connectionPipe;

import java.util.EventListener;

public interface ConnectionPipeListener extends EventListener{
	
	void eventAppeared(ConnectionPipeEvent event);

}
