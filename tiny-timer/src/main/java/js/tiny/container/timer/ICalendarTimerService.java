package js.tiny.container.timer;

import js.tiny.container.spi.IInstancePostConstructionProcessor;
import js.tiny.container.spi.IManagedMethod;

public interface ICalendarTimerService extends IInstancePostConstructionProcessor {
	
	void createTimer(Object instance, IManagedMethod managedMethod);

}
