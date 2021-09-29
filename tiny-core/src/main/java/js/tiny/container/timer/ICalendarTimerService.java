package js.tiny.container.timer;

import js.tiny.container.ManagedMethodSPI;
import js.tiny.container.core.IContainerService;
import js.tiny.container.core.IInstancePostProcessor;

public interface ICalendarTimerService extends IContainerService, IInstancePostProcessor {
	
	void createTimer(Object instance, ManagedMethodSPI managedMethod);

}
