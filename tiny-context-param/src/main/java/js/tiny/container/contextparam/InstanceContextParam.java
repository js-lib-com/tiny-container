package js.tiny.container.contextparam;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import js.converter.Converter;
import js.tiny.container.spi.IInstancePostConstructProcessor;

/**
 * Inject context parameters into instance, non final fields. Field type could be anything for which there is a
 * {@link Converter} registered.
 * 
 * This processor is executed immediately after instance creation but before instance made available to container. Instance
 * fields designed to be initialized should be annotated with the non standard annotation {@link ContextParam}.
 * 
 * @author Iulian Rotaru
 */
public class InstanceContextParam extends BaseContextParam implements IInstancePostConstructProcessor {
	private final Map<Class<?>, Set<Field>> fieldsCache = new HashMap<>();

	@Override
	public Priority getPriority() {
		return Priority.INJECT;
	}

	@Override
	public <T> void onInstancePostConstruct(T instance) {
		final Class<?> implementationClass = instance.getClass();

		Set<Field> fields = fieldsCache.get(implementationClass);
		if (fields == null) {
			synchronized (this) {
				if (fields == null) {
					fields = new HashSet<>();
					fieldsCache.put(implementationClass, fields);

					for (Field field : implementationClass.getDeclaredFields()) {
						if (field.getAnnotation(ContextParam.class) != null && !Modifier.isStatic(field.getModifiers())) {
							fields.add(field);
						}
					}
				}
			}
		}

		for (Field field : fields) {
			setField(field, instance);
		}
	}
}
