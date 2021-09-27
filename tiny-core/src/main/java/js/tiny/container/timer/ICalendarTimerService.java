package js.tiny.container.timer;

import js.tiny.container.ManagedMethodSPI;

public interface ICalendarTimerService {
	
	void createTimer(Object instance, ManagedMethodSPI managedMethod);

	void destroy();

}
