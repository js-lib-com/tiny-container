package js.container;

import js.lang.BugError;

public class CronMethodsProcessor implements InstanceProcessor {
	private final CronManager cronManager;

	public CronMethodsProcessor(CronManager cronManager) {
		this.cronManager = cronManager;
	}

	@Override
	public void postProcessInstance(ManagedClassSPI managedClass, Object instance) {
		if (!managedClass.getCronManagedMethods().iterator().hasNext()) {
			// it is legal for a managed instance to not have any cron methods
			return;
		}
		if (!managedClass.getInstanceScope().equals(InstanceScope.APPLICATION)) {
			throw new BugError("Crom method requires %s instance scope.", InstanceScope.APPLICATION);
		}
		for (ManagedMethodSPI managedMethod : managedClass.getCronManagedMethods()) {
			cronManager.register(instance, managedMethod);
		}
	}
}
