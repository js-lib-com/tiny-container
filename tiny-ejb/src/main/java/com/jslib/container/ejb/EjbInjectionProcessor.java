package com.jslib.container.ejb;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.container.spi.IContainer;
import com.jslib.container.spi.IInstancePostConstructProcessor;
import com.jslib.util.Classes;

public class EjbInjectionProcessor implements IInstancePostConstructProcessor {
	private final static Log log = LogFactory.getLog(EjbInjectionProcessor.class);

	private FieldsCache ejbFields;
	private EjbProxies ejbProxies;

	@Override
	public void create(IContainer container) {
		ejbFields = container.getInstance(FieldsCache.class);
		ejbProxies = container.getInstance(EjbProxies.class);
	}

	@Override
	public Priority getPriority() {
		return Priority.INJECT;
	}

	@Override
	public <T> void onInstancePostConstruct(T instance) {
		ejbFields.getFields(instance.getClass()).forEach(ejbField -> {
			Object ejbProxy = ejbProxies.getProxy(ejbField.getType());
			Classes.setFieldValue(instance, ejbField, ejbProxy);
			log.debug("Inject remote EJB {java_type} into field {}.", ejbField.getType(), ejbField);
		});
	}
}
