package js.tiny.container.timer;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IManagedMethod;

class TimerTask implements Runnable {
	private static final Log log = LogFactory.getLog(TimerTask.class);

	private final CalendarTimerService service;
	private final Object instance;
	private final IManagedMethod managedMethod;

	public TimerTask(CalendarTimerService service, Object instance, IManagedMethod managedMethod) {
		log.trace("TimerTask(service, instance, managedMethod)");
		this.service = service;
		this.instance = instance;
		this.managedMethod = managedMethod;
	}

	@Override
	public void run() {
		log.debug("Execute timer method |%s|.", managedMethod);
		try {
			managedMethod.invoke(instance);
		} catch (Throwable t) {
			log.dump(String.format("Fail on timer method |%s|: ", managedMethod), t);
		}

		long delay = service.computeDelay(managedMethod);
		if (delay > 0) {
			service.schedule(this, delay);
		}
		log.debug("Close timer method |%s|.", managedMethod);
	}
}
