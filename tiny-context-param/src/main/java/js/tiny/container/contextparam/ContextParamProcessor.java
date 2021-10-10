package js.tiny.container.contextparam;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;

import js.lang.BugError;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.servlet.AppContext;
import js.tiny.container.servlet.RequestContext;
import js.tiny.container.spi.IClassPostLoad;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IInstancePostConstruct;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IServiceMeta;

/**
 * Initialize fields depending on context parameters, both class and instance.
 * 
 * @author Iulian Rotaru
 */
public class ContextParamProcessor implements IClassPostLoad, IInstancePostConstruct, IContainerService {
	private static final Log log = LogFactory.getLog(ContextParamProcessor.class);

	/**
	 * Application context used to supply named context parameter, see {@link AppContext#getProperty(String)} and related
	 * {@link AppContext#getProperty(String, Class)}.
	 */
	private final IContainer container;

	/**
	 * Initialize processor instance.
	 * 
	 * @param container application context.
	 */
	public ContextParamProcessor(IContainer container) {
		this.container = container;
	}

	@Override
	public Iterable<IServiceMeta> scan(IManagedClass managedClass) {
		return Collections.emptyList();
	}

	@Override
	public Iterable<IServiceMeta> scan(IManagedMethod managedMethod) {
		return Collections.emptyList();
	}

	@Override
	public void destroy() {
		log.trace("destroy()");
	}

	@Override
	public void postLoadClass(IManagedClass managedClass) {
		RequestContext requestContext = container.getInstance(RequestContext.class);
		for (Field field : managedClass.getImplementationClass().getDeclaredFields()) {
			ContextParam contextParam = field.getAnnotation(ContextParam.class);
			if (contextParam != null) {
				final String parameterName = contextParam.value();
				log.debug("Initialize static field |%s| from context parameter |%s|.", field, parameterName);
				if (Modifier.isStatic(field.getModifiers())) {
					setField(requestContext, field, null, parameterName);
				}
			}
		}
	}

	@Override
	public void postConstructInstance(IManagedClass managedClass, Object instance) {
		RequestContext requestContext = container.getInstance(RequestContext.class);
		for (Field field : managedClass.getImplementationClass().getDeclaredFields()) {
			ContextParam contextParam = field.getAnnotation(ContextParam.class);
			if (contextParam != null) {
				final String parameterName = contextParam.value();
				log.debug("Initialize instance field |%s| from context parameter |%s|.", field, parameterName);
				if (!Modifier.isStatic(field.getModifiers())) {
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
		final Object value = requestContext.getRequest().getServletContext().getInitParameter(parameterName);
		if (value == null) {
			ContextParam contextParam = field.getAnnotation(ContextParam.class);
			if (contextParam == null) {
				throw new BugError("Missing ContextParam annotation from field |%s|.", field);
			}
			if (contextParam.mandatory()) {
				throw new RuntimeException(String.format("Missing context parameter |%s| requested by field |%s|.", contextParam.value(), field));
			}
			log.warn("Field |%s| has no context parameter. Leave it on compiled value.", field);
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
