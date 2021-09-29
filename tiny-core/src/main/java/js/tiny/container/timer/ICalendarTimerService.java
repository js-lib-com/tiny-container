package js.tiny.container.timer;

import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IInstancePostProcessor;
import js.tiny.container.spi.IManagedMethod;

public interface ICalendarTimerService extends IContainerService, IInstancePostProcessor {
	
	void createTimer(Object instance, IManagedMethod managedMethod);

}
