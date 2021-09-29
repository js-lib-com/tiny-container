package js.tiny.container;

import js.lang.BugError;
import js.lang.InvocationException;
import js.lang.NoProviderException;
import js.tiny.container.spi.IManagedClass;

/**
 * Managed instances factory deals with the actual instances creation. Instance creation process may depend on managed class
 * type, see {@link InstanceType}. For example {@link InstanceType#POJO} instances are created by simple invoking
 * <code>newInstance</code> on constructor whereas a {@link InstanceType#REMOTE} instance actually creates a Java Proxy that
 * knows HTTP-RMI protocol.
 * <p>
 * Container uses a strategy to select the instance factory implementation based on managed class type. There are instance
 * factories supported directly by container but is possible to register new managed type and related instance factory. For this
 * one needs to implement <code>InstanceFactory</code> and declare it as Java service into META-INF/services.
 * <p>
 * Implementation should be thread safe and prepared for concurrent execution.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface InstanceFactory {
	/**
	 * Get instance type that uniquely identify this instance factory. Instance type is used as key for selecting instance
	 * factory. It is considered a bug if attempt to register multiple instance factories for the same instance type. 
	 * <p>
	 * <b>Warning:</b> Container fails to start if there are multiple instance factories declared for the same instance type.
	 * 
	 * @return instance type bound to this factory.
	 */
	InstanceType getInstanceType();

	/**
	 * Create new managed instance of the given managed class. This factory method always creates a new instance. Optional
	 * arguments are passed to constructor but not all implementations supports arguments. For example, instance factory for
	 * {@link InstanceType#REMOTE} does not support constructor arguments. Implementation should throw {@link BugError} if it
	 * does not support arguments but caller provides them.
	 * 
	 * @param managedClass managed class,
	 * @param args optional constructor arguments.
	 * @param <T> instance type.
	 * @return newly created instance or null.
	 * @throws IllegalArgumentException if <code>args</code> parameter does not respect constructor signature or implementation
	 *             does not support arguments but caller provides them.
	 * @throws InvocationException if implementation is local and constructor execution fails.
	 * @throws NoProviderException if interface is a service and no provider found on run-time.
	 */
	<T> T newInstance(IManagedClass managedClass, Object... args);
}
