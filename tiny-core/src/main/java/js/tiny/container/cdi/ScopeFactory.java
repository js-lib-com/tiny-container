package js.tiny.container.cdi;

import js.tiny.container.core.InstanceKey;
import js.tiny.container.core.InstanceScope;

/**
 * Strategy for managed instances life span management, also known as managed instance <code>scope</code>. This interface is
 * part of a strategy used by container to select the scope specific implementation. Implementation should have some means to
 * store and reuse instances. First method, {@link #getInstance(InstanceKey)} will retrieve stored instance or null if none
 * persisted. The second, {@link #persistInstance(InstanceKey, Object)} is used to actually save an instance into internal
 * storage.
 * <p>
 * Implementation is not required to be thread safe since container synchronize access to scope factory.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface ScopeFactory {
	/**
	 * Get instance scope that uniquely identify this scope factory. Instance scope is used as key for selecting scope factory.
	 * It is considered a bug if attempt to register multiple scope factories for the same instance scope.
	 * <p>
	 * <b>Warning:</b> Container fails to start if there are multiple scope factories declared for the same instance scope.
	 * 
	 * @return instance scope bound to this factory.
	 */
	InstanceScope getInstanceScope();

	/**
	 * Get instance from internal storage or null if no instance bound to managed class. Implementation should use provided
	 * instance key for managed instance search.
	 * 
	 * @param instanceKey managed instance key.
	 * @return persisted managed instance or null if none found.
	 */
	Object getInstance(InstanceKey instanceKey);

	/**
	 * Persist instance bound to given instance key. Implementation should use provided instance key for instance persistence.
	 * 
	 * @param instanceKey managed instance key,
	 * @param instance managed instance, never null.
	 */
	void persistInstance(InstanceKey instanceKey, Object instance);

	/** Clear this scope factory internal cache. */
	void clear();
}
