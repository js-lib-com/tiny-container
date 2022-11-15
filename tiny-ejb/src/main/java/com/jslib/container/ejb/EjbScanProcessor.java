package com.jslib.container.ejb;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.jslib.container.spi.IClassPostLoadedProcessor;
import com.jslib.container.spi.IContainer;
import com.jslib.container.spi.IManagedClass;
import com.jslib.container.spi.ServiceConfigurationException;
import com.jslib.net.client.HttpRmiFactory;
import com.jslib.rmi.RemoteFactory;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.ApplicationScoped;

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
					throw new ServiceConfigurationException("Attempt to inject EJB in final field |%s#%s|.", implementationClass.getCanonicalName(), field.getName());
				}
				if (Modifier.isStatic(field.getModifiers())) {
					throw new ServiceConfigurationException("Attempt to inject EJB in static field |%s#%s|.", implementationClass.getCanonicalName(), field.getName());
				}
				if(!field.getType().isInterface()) {
					throw new ServiceConfigurationException("EJB field |%s#%s| must be an interface.", implementationClass.getCanonicalName(), field.getName());
				}
				createManagedClass = true;
				cache.add(implementationClass, field);
			}
		}
		return createManagedClass;
	}
}
