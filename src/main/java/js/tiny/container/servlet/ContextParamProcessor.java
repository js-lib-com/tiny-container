package js.tiny.container.servlet;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

import js.lang.BugError;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.ClassProcessor;
import js.tiny.container.InstanceProcessor;
import js.tiny.container.ManagedClassSPI;
import js.tiny.container.core.AppContext;

/**
 * Initialize fields depending on context parameters, both class and instance.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public class ContextParamProcessor implements ClassProcessor, InstanceProcessor {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(ContextParamProcessor.class);

	/**
	 * Application context used to supply named context parameter, see {@link AppContext#getProperty(String)} and related
	 * {@link AppContext#getProperty(String, Class)}.
	 */
	private final AppContext context;

	/**
	 * Initialize processor instance.
	 * 
	 * @param context application context.
	 */
	public ContextParamProcessor(AppContext context) {
		this.context = context;
	}

	@Override
	public void postProcessClass(ManagedClassSPI managedClass) {
		for (Map.Entry<String, Field> entry : managedClass.getContextParamFields().entrySet()) {
			final String parameterName = entry.getKey();
			final Field field = entry.getValue();
			log.debug("Initialize static field |%s| from context parameter |%s|.", field, parameterName);
			if (Modifier.isStatic(field.getModifiers())) {
				setField(field, parameterName, null);
			}
		}
	}

	@Override
	public void postProcessInstance(ManagedClassSPI managedClass, Object instance) {
		for (Map.Entry<String, Field> entry : managedClass.getContextParamFields().entrySet()) {
			final String parameterName = entry.getKey();
			final Field field = entry.getValue();
			log.debug("Initialize instance field |%s| from context parameter |%s|.", field, parameterName);
			if (!Modifier.isStatic(field.getModifiers())) {
				setField(field, parameterName, instance);
			}
		}
	}

	/**
	 * Initialize field from named context parameter.
	 * 
	 * @param field field to be initialized, both class and instance fields accepted,
	 * @param parameterName name for context parameter,
	 * @param instance optional instance, null for class fields.
	 */
	private void setField(Field field, String parameterName, Object instance) {
		final Object value = context.getProperty(parameterName, field.getType());
		try {
			field.set(instance, value);
		} catch (Exception e) {
			throw new BugError(e);
		}
	}
}
