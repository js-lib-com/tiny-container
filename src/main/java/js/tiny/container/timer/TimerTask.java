package js.tiny.container.timer;

import js.lang.InvocationException;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.AuthorizationException;
import js.tiny.container.ManagedMethodSPI;

class TimerTask implements Runnable {
	private static final Log log = LogFactory.getLog(TimerTask.class);

	private final CalendarTimerService service;
	private final Object instance;
	private final ManagedMethodSPI managedMethod;

	public TimerTask(CalendarTimerService service, Object instance, ManagedMethodSPI managedMethod) {
		log.trace("TimerTask(service, instance, managedMethod)");
		this.service = service;
		this.instance = instance;
		this.managedMethod = managedMethod;
	}

	@Override
	public void run() {
		log.trace("run()");
		log.debug("Execute timer method |%s|.", managedMethod);
		try {
			managedMethod.invoke(instance);
			long delay = service.computeDelay(managedMethod.getSchedule());
			if (delay > 0) {
				service.schedule(this, delay);
			}
		} catch (IllegalArgumentException | InvocationException | AuthorizationException e) {
			log.error(e);
		} catch (Throwable t) {
			log.error(t);
		}
	}
}
