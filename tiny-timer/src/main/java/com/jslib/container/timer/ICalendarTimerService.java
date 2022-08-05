package com.jslib.container.timer;

import com.jslib.container.spi.IInstancePostConstructProcessor;
import com.jslib.container.spi.IManagedMethod;

public interface ICalendarTimerService extends IInstancePostConstructProcessor {
	
	void createTimer(Object instance, IManagedMethod managedMethod);

}
