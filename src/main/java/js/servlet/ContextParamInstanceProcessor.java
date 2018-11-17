package js.servlet;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

import js.container.InstanceProcessor;
import js.container.ManagedClassSPI;
import js.core.AppContext;
import js.lang.BugError;
import js.log.Log;
import js.log.LogFactory;

public class ContextParamInstanceProcessor implements InstanceProcessor {
	private static final Log log = LogFactory.getLog(ContextParamInstanceProcessor.class);

	private final AppContext context;

	public ContextParamInstanceProcessor(AppContext context) {
		this.context = context;
	}

	@Override
	public void postProcessInstance(ManagedClassSPI managedClass, Object instance) {
		for (Map.Entry<String, Field> entry : managedClass.getContextParamFields().entrySet()) {
			final String parameterName = entry.getKey();
			final Field field = entry.getValue();
			log.debug("Initialize field |%s| from context parameter |%s|.", field, parameterName);

			final Object value = context.getProperty(parameterName, field.getType());

			try {
				if (Modifier.isStatic(field.getModifiers())) {
					field.set(null, value);
				} else {
					field.set(instance, value);
				}
			} catch (Exception e) {
				throw new BugError(e);
			}
		}
	}
}
