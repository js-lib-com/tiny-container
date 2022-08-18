package com.jslib.container.ejb;

import java.util.function.BiConsumer;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.container.spi.IContainer;
import com.jslib.container.spi.IInstancePostConstructProcessor;
import com.jslib.rmi.InvocationProperties;
import com.jslib.rmi.RemoteFactory;
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
			Object remoteInstance = factory.getRemoteInstance((Class) ejb.getType(), ejb.getImplementationURL(), () -> {
				return new InvocationProperties() {
					@Override
					public void forEach(BiConsumer<String, Object> consumer) {
						String traceId = LogFactory.getLogContext().get("trace_id");
						if (traceId != null) {
							consumer.accept("X-Trace-Id", traceId);
						}
					}
				};
			});

			Classes.setFieldValue(instance, ejb.getField(), remoteInstance);
			log.debug("Inject remote EJB |{ejb_class}| into field |{java_type}#{java_field}|.", ejb, instance.getClass(), ejb.getField().getName());
		});
	}
}
