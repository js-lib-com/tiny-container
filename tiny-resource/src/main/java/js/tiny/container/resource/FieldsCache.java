package js.tiny.container.resource;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Resource;
import js.lang.BugError;

/**
 * Cache for implementation class fields marked with <code>@Resource</code> annotation, both Jakarta and Javax. This cache
 * considers all fields, no matter private, protected or public. Anyway it is considered a bug if resource annotation is found
 * on final or static field.
 * 
 * @author Iulian Rotaru
 */
public class FieldsCache {
	/** Cache for class resource fields. */
	private final Map<Class<?>, Set<Field>> cache = new HashMap<>();

	/**
	 * Get resource fields for requested implementation class from internal cache. On cache miss delegate
	 * {@link #scanFields(Class)}. Resource fields are those annotated with <code>@Resource</code>, both Jakarta and Javax
	 * supported.
	 * 
	 * @param implementationClass class to scan for resource fields, null tolerated.
	 * @return fields collection, in no particular order.
	 * @throws BugError if <code>@Resource</code> annotation is used on a final or static field.
	 */
	public Collection<Field> get(Class<?> implementationClass) {
		Set<Field> fields = cache.get(implementationClass);
		if (fields == null) {
			synchronized (this) {
				fields = cache.get(implementationClass);
				if (fields == null) {
					fields = scanFields(implementationClass);
					cache.put(implementationClass, fields);
				}
			}
		}
		return fields;
	}

	/**
	 * Scan class fields declared by {@link Resource} annotation, both Jakarta and Javax. This method scans all fields, no
	 * matter private, protected or public. Anyway it is considered a bug if resource annotation is found on final or static
	 * field.
	 * 
	 * Returns a set of reflective fields with accessibility set but in not particular order. If given implementation class
	 * argument is null returns empty collection.
	 * 
	 * @param implementationClass class to scan for resource fields, null tolerated.
	 * @return fields collection, in no particular order.
	 * @throws BugError if <code>@Resource</code> annotation is used on a final or static field.
	 */
	static Set<Field> scanFields(Class<?> implementationClass) {
		if (implementationClass == null) {
			return Collections.emptySet();
		}

		Set<Field> fields = new HashSet<>();
		for (Field field : implementationClass.getDeclaredFields()) {
			if (field.isSynthetic()) {
				// it seems there can be injected fields, created via byte code manipulation, when run with test coverage active
				// not clear why and how but was consistently observed on mock object from unit test run with coverage
				continue;
			}
			if (IResource.scan(field) == null) {
				continue;
			}

			if (Modifier.isFinal(field.getModifiers())) {
				throw new BugError("Attempt to inject resource in final field |%s|.", field.getName());
			}
			if (Modifier.isStatic(field.getModifiers())) {
				throw new BugError("Attempt to inject resource in static field |%s|.", field.getName());
			}
			field.setAccessible(true);
			fields.add(field);
		}
		return fields;
	}
}
