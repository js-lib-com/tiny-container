package js.tiny.container.timer;

import js.tiny.container.spi.IInstancePostConstructProcessor;
import js.tiny.container.spi.IManagedMethod;

public interface ICalendarTimerService extends IInstancePostConstructProcessor {
	
	void createTimer(Object instance, IManagedMethod managedMethod);

}
