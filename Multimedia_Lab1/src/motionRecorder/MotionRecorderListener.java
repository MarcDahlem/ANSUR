package motionRecorder;

import java.util.EventListener;

public interface MotionRecorderListener extends EventListener{
	
	void eventAppeared(MotionRecorderEvent event);

}
