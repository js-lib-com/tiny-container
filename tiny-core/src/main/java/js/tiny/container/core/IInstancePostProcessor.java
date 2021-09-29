package js.tiny.container.core;

import js.lang.BugError;
import js.tiny.container.InstanceFactory;
import js.tiny.container.ManagedClassSPI;
import js.tiny.container.ScopeFactory;

/**
 * Post processor for instances created by {@link InstanceFactory}. Instance processors are responsible for services provided by
 * container at instance level. Instance processors are registered to container and enacted by instance retrieval logic. Note
 * that post processing is executed only on newly created instances but not if managed instance is reused from
 * {@link ScopeFactory}.
 * <p>
 * Instance processor may have side effects on given instance, depending on specific implementation. For example
 * {@link LoggerInstanceProcessor} does not alter given instance whereas {@link InstanceFieldsInjectionProcessor} does inject
 * dependencies, altering instance state.
 * <p>
 * Is not allowed for implementation to throw exceptions since there are no expectable conditions that can prevent instance
 * post-processing. Anyway, implementation can throw unchecked exceptions or errors. It is recommended for implementation to
 * treat all erroneous conditions as bugs and throw {@link BugError}.
 * 
 * @author Iulian Rotaru
 */
public interface IInstancePostProcessor extends IJoinPointProcessor {
	/**
	 * Execute specific post processing on instance of a given managed class. Implementation may or may not alter instance
	 * state, depending on specific kind of processing. For example {@link LoggerInstanceProcessor} does not alter instance
	 * whereas {@link InstanceFieldsInjectionProcessor} does inject dependencies altering instance state. Both managed class and
	 * instance arguments are guaranteed to be non null.
	 * 
	 * @param managedClass managed class,
	 * @param instance instance of given managed class.
	 * @throws BugError for every abnormal condition that prevent post-processing.
	 */
	void postProcessInstance(ManagedClassSPI managedClass, Object instance);
}
