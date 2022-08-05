package com.jslib.tiny.container.ejb;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;

import com.jslib.rmi.RemoteFactory;
import com.jslib.tiny.container.spi.IContainer;
import com.jslib.tiny.container.spi.IInstancePostConstructProcessor;
import com.jslib.util.Classes;

public class EjbInjectionProcessor implements IInstancePostConstructProcessor {
	private final static Log log = LogFactory.getLog(EjbInjectionProcessor.class);

	private RemoteFactory factory;
	private FieldsCache cache;

	@Override
	public void create(IContainer container) {
		factory = container.getInstance(RemoteFactory.class);
		cache = container.getInstance(FieldsCache.class);
	}

	@Override
	public Priority getPriority() {
		return Priority.INJECT;
	}

	@Override
	public <T> void onInstancePostConstruct(T instance) {
		cache.get(instance.getClass()).forEach(ejb -> {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Object remoteInstance = factory.getRemoteInstance((Class) ejb.getType(), ejb.getImplementationURL());
			Classes.setFieldValue(instance, ejb.getField(), remoteInstance);
			log.debug("Inject remote EJB |%s| into field |%s#%s|.", ejb, instance.getClass().getCanonicalName(), ejb.getField().getName());
		});
	}
}
