package js.tiny.container.cdi;

import java.util.HashMap;
import java.util.Map;

import js.tiny.container.core.InstanceKey;
import js.tiny.container.core.InstanceScope;

/**
 * Scope factory for application level singletons. Application scope factory has an {@link #instancesPool} and attempt to
 * retrieve managed instance from it via {@link #getInstance(InstanceKey)}; instances pool is updated by
 * {@link #persistInstance(InstanceKey, Object)}. Both uses the key provided managed instance key.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public class ApplicationScopeFactory implements ScopeFactory {
	/**
	 * Managed instances pool for managed classes with application scope. Note that this cache has application scope only if
	 * this factory has; anyway, container takes care to create a single instance of any scope factory, including this.
	 */
	private Map<InstanceKey, Object> instancesPool = new HashMap<>();

	@Override
	public InstanceScope getInstanceScope() {
		return InstanceScope.APPLICATION;
	}

	/**
	 * Retrieve instance bound to given managed instance key or null if none found. Uses <code>instanceKey</code> argument to
	 * get instance from {@link #instancesPool}. Managed instance key argument should be not null.
	 * 
	 * @param instanceKey managed instance key.
	 * @return managed instance or null.
	 */
	@Override
	public Object getInstance(InstanceKey instanceKey) {
		// at this point managed instance key is guaranteed to be non null
		return instancesPool.get(instanceKey);
	}

	/**
	 * Persist instance bound to given managed instance key. This method simply uses provided <code>instanceKey</code> argument
	 * to add instance to {@link #instancesPool}. Both arguments should to be not null.
	 * 
	 * @param instanceKey managed instance key,
	 * @param instance managed instance.
	 */
	@Override
	public void persistInstance(InstanceKey instanceKey, Object instance) {
		// at this point managed class and instance are guaranteed to be non null
		instancesPool.put(instanceKey, instance);
	}

	/** Clear {@link #instancesPool}. */
	@Override
	public void clear() {
		instancesPool.clear();
	}
}