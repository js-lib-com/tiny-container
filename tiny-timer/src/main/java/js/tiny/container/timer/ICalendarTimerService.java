package js.tiny.container.timer;

import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IInstancePostConstruct;
import js.tiny.container.spi.IManagedMethod;

public interface ICalendarTimerService extends IContainerService, IInstancePostConstruct {
	
	void createTimer(Object instance, IManagedMethod managedMethod);

}
