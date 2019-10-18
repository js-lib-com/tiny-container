package js.tiny.container;

import java.lang.reflect.Field;

import js.lang.BugError;
import js.tiny.container.annotation.Inject;
import js.util.Classes;

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
		for (Field dependency : managedClass.getDependencies()) {
			if (dependency.isSynthetic()) {
				// it seems there can be injected fields, created via byte code manipulation, when run with test coverage active
				// not clear why and how but was consistently observed on mock object from unit test run with coverage
				continue;
			}
			Classes.setFieldValue(instance, dependency, getDependencyValue(managedClass, dependency.getType()));
		}
	}
}
