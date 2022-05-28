package js.tiny.container.ejb;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.ApplicationScoped;
import js.lang.BugError;
import js.net.client.HttpRmiFactory;
import js.rmi.RemoteFactory;
import js.tiny.container.spi.IClassPostLoadedProcessor;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IManagedClass;

public class EjbScanProcessor implements IClassPostLoadedProcessor {
	private FieldsCache cache;

	@Override
	public void configure(IContainer container) {
		container.bind(RemoteFactory.class).to(HttpRmiFactory.class).in(ApplicationScoped.class).build();
		container.bind(FieldsCache.class).in(ApplicationScoped.class).build();
	}

	@Override
	public void create(IContainer container) {
		cache = container.getInstance(FieldsCache.class);
	}

	@Override
	public Priority getPriority() {
		return Priority.SCAN;
	}

	@Override
	public <T> boolean onClassPostLoaded(IManagedClass<T> managedClass) {
		boolean createManagedClass = false;
		Class<? extends T> implementationClass = managedClass.getImplementationClass();
		for (Field field : implementationClass.getDeclaredFields()) {
			if (field.getAnnotation(EJB.class) != null) {
				if (Modifier.isFinal(field.getModifiers())) {
					throw new BugError("Attempt to inject EJB in final field |%s#%s|.", implementationClass.getCanonicalName(), field.getName());
				}
				if (Modifier.isStatic(field.getModifiers())) {
					throw new BugError("Attempt to inject EJB in static field |%s#%s|.", implementationClass.getCanonicalName(), field.getName());
				}
				createManagedClass = true;
				cache.add(implementationClass, field);
			}
		}
		return createManagedClass;
	}
}