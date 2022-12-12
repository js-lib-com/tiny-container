package com.jslib.container.rest;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.container.spi.IContainer;
import com.jslib.container.spi.IInstancePostConstructProcessor;

public class ContextInjectionProcessor implements IInstancePostConstructProcessor {
	private static final Log log = LogFactory.getLog(ContextInjectionProcessor.class);

	private IContainer container;
	private ContextInjectionCache cache;

	@Override
	public void create(IContainer container) {
		this.container = container;
		this.cache = container.getInstance(ContextInjectionCache.class);
	}

	@Override
	public Priority getPriority() {
		return Priority.INJECT;
	}

	@Override
	public <T> void onInstancePostConstruct(T instance) {
		for (IMemberInjector injector : cache.get(instance.getClass())) {
			Object value = container.getInstance(injector.type());
			try {
				injector.inject(instance, value);
			} catch (Throwable e) {
				String message = String.format("Fail to inject context |%s| on |%s|.", injector.type(), injector);
				log.dump(message, e);
				throw new ContextInjectionException(message);
			}
		}
	}
}
