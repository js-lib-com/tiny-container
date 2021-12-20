package js.tiny.container.contextparam;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import js.lang.BugError;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.servlet.RequestContext;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;

abstract class BaseContextParam implements IContainerService {
	private static final Log log = LogFactory.getLog(BaseContextParam.class);

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
			throw new IllegalStateException(String.format("Attempt to initialize final field |%s|.", field));
		}

		ContextParam contextParam = field.getAnnotation(ContextParam.class);
		assert contextParam != null;
		final String contextParameterName = contextParam.value();
		log.debug("Initialize field |%s| from context parameter |%s|.", field, contextParameterName);

		final RequestContext requestContext = container.getInstance(RequestContext.class);
		final Object value = requestContext.getInitParameter(field.getType(), contextParameterName);
		if (value == null) {
			if (contextParam.mandatory()) {
				throw new RuntimeException(String.format("Missing context parameter |%s| requested by field |%s|.", contextParameterName, field));
			}
			log.warn("Field |%s| has no context parameter. Leave it unchanged.", field);
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
