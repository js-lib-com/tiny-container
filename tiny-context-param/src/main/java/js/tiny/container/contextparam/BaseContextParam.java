package js.tiny.container.contextparam;

import java.lang.reflect.Field;
import java.util.function.Predicate;

import js.lang.BugError;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.servlet.RequestContext;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IManagedClass;

abstract class BaseContextParam implements IContainerService {
	private static final Log log = LogFactory.getLog(BaseContextParam.class);

	private IContainer container;

	@Override
	public void create(IContainer container) {
		log.trace("create(IContainer)");
		this.container = container;
	}

	protected void processFields(IManagedClass<?> managedClass, Object instance, Predicate<Field> predicate) {
		RequestContext requestContext = container.getInstance(RequestContext.class);
		for (Field field : managedClass.getImplementationClass().getDeclaredFields()) {
			ContextParam contextParam = field.getAnnotation(ContextParam.class);
			if (contextParam != null) {
				final String parameterName = contextParam.value();
				log.debug("Initialize %s field |%s| from context parameter |%s|.", instance == null ? "static" : "instance", field, parameterName);
				if (predicate.test(field)) {
					setField(requestContext, field, instance, parameterName);
				}
			}
		}
	}

	/**
	 * Initialize field from named context parameter.
	 *
	 * @param requestContext request context attached to current thread,
	 * @param field field to be initialized, both class and instance fields accepted,
	 * @param instance optional instance, null for class fields,
	 * @param parameterName name for context parameter.
	 */
	private static void setField(RequestContext requestContext, Field field, Object instance, String parameterName) {
		final Object value = requestContext.getInitParameter(field.getType(), parameterName);
		if (value == null) {
			ContextParam contextParam = field.getAnnotation(ContextParam.class);
			assert contextParam != null;
			if (contextParam.mandatory()) {
				throw new RuntimeException(String.format("Missing context parameter |%s| requested by field |%s|.", contextParam.value(), field));
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
