package js.tiny.container;

import java.util.HashMap;
import java.util.Map;

import js.lang.BugError;

/**
 * Scope factory that creates thread local instances. An instance with {@link InstanceScope#THREAD}, once created, has the same
 * value for entire thread life span. If attempt to retrieve it again, from the same thread, its value is reused.
 * <p>
 * Thread scope factory has an {@link #instancesPool} and attempt to retrieve managed instance from it, see
 * {@link #getInstance(InstanceKey)}; uses {@link #persistInstance(InstanceKey, Object)} to update instances pool. Both uses
 * provided instance key argument to store and retrieve instances.
 * <p>
 * Instances pool uses thread local storage to keep instances but is not inheritable, that is, thread local instance is not
 * visible on child threads.
 * 
 * @author Iulian Rotaru
 * @version final
 */
final class ThreadScopeFactory implements ScopeFactory {
	/**
	 * Managed instances pool for managed classes with thread scope. A managed class has a thread local storage created on the
	 * fly. Once instance saved into thread local, it is reused for entire thread life span. Implementation should be aware not
	 * to use {@link InheritableThreadLocal}.
	 */
	private Map<InstanceKey, ThreadLocal<Object>> instancesPool = new HashMap<>();

	@Override
	public InstanceScope getInstanceScope() {
		return InstanceScope.THREAD;
	}

	/**
	 * Retrieve instance from current thread, bound to given managed class or null if none found. Uses provided managed instance
	 * key to get instance from {@link #instancesPool}. Instance key argument should be not null.
	 * 
	 * @param instanceKey managed instance key.
	 * @return current thread managed instance or null.
	 */
	@Override
	public Object getInstance(InstanceKey instanceKey) {
		// at this point managed class is guaranteed to be non null

		ThreadLocal<Object> tls = instancesPool.get(instanceKey);
		if (tls == null) {
			synchronized (instancesPool) {
				tls = instancesPool.get(instanceKey);
				if (tls == null) {
					tls = new ThreadLocal<>();
					instancesPool.put(instanceKey, tls);
				}
			}
			return null;
		}

		return tls.get();
	}

	/**
	 * Persist instance on current thread bound to given managed class. This method simply uses provided instance key argument
	 * to add instance to {@link #instancesPool}. Both arguments should to be not null.
	 * 
	 * @param instanceKey managed instance key,
	 * @param instance managed instance.
	 */
	@Override
	public void persistInstance(InstanceKey instanceKey, Object instance) {
		// at this point managed class and instance are guaranteed to be non null

		ThreadLocal<Object> tls = instancesPool.get(instanceKey);
		if (tls == null) {
			throw new BugError("Invalid methods invocation sequence. Ensure getInstance() is invoked before and is executed in the same thread.");
		}
		tls.set(instance);
	}

	/** Clear all threads local storage and {@link #instancesPool}. */
	@Override
	public void clear() {
		for (ThreadLocal<Object> threadLocal : instancesPool.values()) {
			threadLocal.remove();
		}
		instancesPool.clear();
	}
}