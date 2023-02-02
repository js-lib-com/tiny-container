package com.jslib.container.timer;

import java.lang.reflect.Method;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.container.spi.CT;

class TimerTask implements Runnable {
	private static final Log log = LogFactory.getLog(TimerTask.class);

	private static final String SERVICE_NAME = "timer-service";
	
	private final String appName;
	private final String contextName;

	private final CalendarTimerService service;
	private final Object instance;
	private final Method method;

	public TimerTask(CalendarTimerService service, Object instance, Method method) {
		log.trace("TimerTask(CalendarTimerService, Object, Method)");

		this.appName = LogFactory.getLogContext().get(CT.LOG_APP_NAME);
		this.contextName = LogFactory.getLogContext().get(CT.LOG_CONTEXT_NAME);

		this.service = service;
		this.instance = instance;
		this.method = method;
	}

	@Override
	public void run() {
		long start = System.nanoTime();
		LogFactory.getLogContext().put(CT.LOG_APP_NAME, appName);
		LogFactory.getLogContext().put(CT.LOG_CONTEXT_NAME, contextName);
		LogFactory.getLogContext().put(CT.LOG_SERVICE_NAME, SERVICE_NAME);

		log.trace("Execute timer method {}.", method);
		try {
			method.invoke(instance);
		} catch (Throwable t) {
			log.dump(t);
		}

		long delayMillis = service.computeDelay(method);
		if (delayMillis > 0) {
			service.schedule(this, delayMillis);
		}
		log.debug("Close timer method {}. Processing time {processing_time} msec.", method, (System.nanoTime() - start) / 1000000.0);
	}
}
