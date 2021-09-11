package js.tiny.container;

import java.lang.reflect.Field;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import js.lang.BugError;
import js.log.Log;
import js.log.LogFactory;
import js.util.Classes;
import js.util.Strings;

/**
 * Post processor for instance fields injection. Fields are discovered by managed class based on {@link Inject} annotation and
 * provided to this processor by {@link ManagedClassSPI#getDependencies()}. This class inherits dependency processor and
 * delegates {@link DependencyProcessor#getDependencyValue(ManagedClassSPI, Class)} for dependency value processing.
 * <p>
 * In essence this processor scans all dependencies detected by managed class and for every field retrieve its dependency value
 * and inject it reflexively.
 * 
 * @author Iulian Rotaru
 * @version final
 */
final class InstanceFieldsInjectionProcessor extends DependencyProcessor implements InstanceProcessor {
	private static final Log log = LogFactory.getLog(InstanceFieldsInitializationProcessor.class);

	private final Context globalEnvironment;
	private final Context componentEnvironment;

	public InstanceFieldsInjectionProcessor() {
		this.globalEnvironment = environment("java:global/env");
		this.componentEnvironment = environment("java:comp/env");
	}

	private static Context environment(String name) {
		Context context = null;
		try {
			Context serverContext = new InitialContext();
			context = (Context) serverContext.lookup(name);
		} catch (NamingException e) {
			log.error(e);
		}
		return context;
	}

	/**
	 * Inject dependencies described by given managed class into related managed instance. For every dependency field retrieve
	 * its value using {@link DependencyProcessor#getDependencyValue(ManagedClassSPI, Class)} and inject it reflexively.
	 * 
	 * @param managedClass managed class,
	 * @param instance instance of given managed class.
	 * @throws BugError if a field dependency cannot be resolved.
	 */
	@Override
	public void postProcessInstance(ManagedClassSPI managedClass, Object instance) {
		if (instance == null) {
			// null instance is silently ignored since container ensure not null instance argument
			return;
		}
		for (Field field : managedClass.getDependencies()) {
			if (field.isSynthetic()) {
				// it seems there can be injected fields, created via byte code manipulation, when run with test coverage active
				// not clear why and how but was consistently observed on mock object from unit test run with coverage
				continue;
			}

			Object value = null;
			Resource resourceAnnotation = field.getAnnotation(Resource.class);
			if (resourceAnnotation != null) {
				value = getEnvEntryValue(field, resourceAnnotation);
			} else {
				value = getDependencyValue(managedClass, field.getType());
			}

			if (value == null) {
				log.debug("Null dependency for field |%s|. Leave it unchanged.", field);
				return;
			}

			Classes.setFieldValue(instance, field, value);
			log.debug("Inject field |%s| value |%s|.", field, value);
		}
	}

	private Object getEnvEntryValue(Field field, Resource resourceAnnotation) {
		String lookupName = resourceAnnotation.lookup();
		if (!lookupName.isEmpty()) {
			Object value = null;
			if (globalEnvironment != null) {
				try {
					value = globalEnvironment.lookup(lookupName);
					log.debug("Load global resource |%s| of type |%s|.", lookupName, field.getType());
				} catch (NamingException e) {
					throw new BugError(e);
				}
			}
			return value;
		}

		if (componentEnvironment == null) {
			return null;
		}
		String name = resourceAnnotation.name();
		if (name.isEmpty()) {
			name = Strings.concat(field.getDeclaringClass().getName(), '/', field.getName());
		}
		if (name.startsWith("java:")) {
			throw new IllegalArgumentException("Resource name should be relative to component environment: " + name);
		}

		Object value = null;
		try {
			value = componentEnvironment.lookup(name);
			log.debug("Load resource |java:/comp/env/%s| of type |%s|.", name, field.getType());
		} catch (NamingException e) {
			throw new BugError(e);
		}
		return value;
	}
}
