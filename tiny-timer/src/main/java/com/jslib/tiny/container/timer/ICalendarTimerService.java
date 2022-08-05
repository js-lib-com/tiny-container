package com.jslib.tiny.container.timer;

import com.jslib.tiny.container.spi.IInstancePostConstructProcessor;
import com.jslib.tiny.container.spi.IManagedMethod;

public interface ICalendarTimerService extends IInstancePostConstructProcessor {
	
	void createTimer(Object instance, IManagedMethod managedMethod);

}
