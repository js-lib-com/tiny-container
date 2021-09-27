package js.tiny.container.timer;

import js.lang.BugError;
import js.tiny.container.InstanceProcessor;
import js.tiny.container.InstanceScope;
import js.tiny.container.ManagedClassSPI;
import js.tiny.container.ManagedMethodSPI;

public class TimerMethodsProcessor implements InstanceProcessor {
	private final ICalendarTimerService timerService;

	public TimerMethodsProcessor(ICalendarTimerService timerService) {
		this.timerService = timerService;
	}

	@Override
	public void postProcessInstance(ManagedClassSPI managedClass, Object instance) {
		if (!managedClass.getCronMethods().iterator().hasNext()) {
			// it is legal for a managed instance to not have any cron methods
			return;
		}
		if (!managedClass.getInstanceScope().equals(InstanceScope.APPLICATION)) {
			throw new BugError("Crom method requires %s instance scope.", InstanceScope.APPLICATION);
		}
		for (ManagedMethodSPI managedMethod : managedClass.getCronMethods()) {
			timerService.createTimer(instance, managedMethod);
		}
	}
}
