package com.jslib.container.contextparam;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.container.spi.IContainer;
import com.jslib.container.spi.IContainerService;
import com.jslib.container.spi.ServiceConfigurationException;
import com.jslib.converter.ConverterRegistry;
import com.jslib.lang.BugError;
import com.jslib.util.Classes;

/**
 * Common logic for both static and instance context parameter injection.
 * 
 * @author Iulian Rotaru
 */
abstract class BaseContextParam implements IContainerService {
	private static final Log log = LogFactory.getLog(BaseContextParam.class);

	private final ConverterRegistry converterRegistry = ConverterRegistry.getInstance();
	private IContainer container;

	@Override
	public void create(IContainer container) {
		log.trace("create(IContainer)");
		this.container = container;
	}

	/**
	 * Initialize field from named context parameter.
	 *
	 * @param contextParam context parameter annotation,
	 * @param field field to be initialized, both class and instance fields accepted,
	 * @param instance optional instance, ignored for class static fields.
	 */
	protected void setField(Field field, Object instance) {
		if (Modifier.isFinal(field.getModifiers())) {
			throw new ServiceConfigurationException("Attempt to initialize final field |%s|.", field);
		}

		ContextParam contextParam = field.getAnnotation(ContextParam.class);
		assert contextParam != null;
		final String contextParameterName = contextParam.name();
		log.debug("Initialize field |{java_field}| from context parameter |{context_parameter}|.", field, contextParameterName);

		Object value = null;
		if (converterRegistry.hasClassConverter(field.getType())) {
			value = container.getInitParameter(contextParameterName, field.getType());
		}

		if (!contextParam.parser().equals(ContextParam.NullParser.class)) {
			ContextParam.Parser parser = Classes.newInstance(contextParam.parser());
			try {
				value = parser.parse(container.getInitParameter(contextParameterName, String.class));
			} catch (Exception e) {
				log.error("Fail to parse context parameter |{context_parameter}| using |{java_type}|", contextParameterName, contextParam.parser().getCanonicalName());
				value = null;
			}
		}

		if (value == null) {
			if (contextParam.mandatory()) {
				throw new NoContextParamException("Missing context parameter |{context_parameter}| requested by field |{java_field}|.", contextParameterName, field);
			}
			log.warn("Field |{java_field}| has no context parameter. Leave it unchanged.", field);
			return;
		}

		field.setAccessible(true);
		try {
			field.set(instance, value);
		} catch (Exception e) {
			throw new BugError(e);
		}
	}
}
