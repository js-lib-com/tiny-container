package js.tiny.container.cdi;

import js.lang.BugError;
import js.lang.NoProviderException;
import js.tiny.container.core.InstanceType;
import js.tiny.container.spi.IManagedClass;
import js.util.Classes;

/**
 * Built-in factory for service instances. This instances factory deals with {@link InstanceType#SERVICE} managed classes. It
 * uses managed class interface to locate service and load it. Throws {@link NoProviderException} if no service implementation
 * found on run-time.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public class ServiceInstanceFactory implements InstanceFactory {
	/**
	 * Service instances factory is built-in and does not have a type name.
	 * 
	 * @throws UnsupportedOperationException this operation is not supported.
	 */
	@Override
	public InstanceType getInstanceType() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Load service defined by managed class interface and return service instance. This factory does not support arguments.
	 * Service provider should be present into run-time otherwise no provider exception is thrown.
	 * 
	 * @param managedClass managed class,
	 * @param args service instances factory does not support arguments.
	 * @return loaded service instance.
	 * @throws IllegalArgumentException if <code>args</code> argument is not empty.
	 * @throws NoProviderException if no service provider found on run-time for requested interface.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <I> I newInstance(IManagedClass managedClass, Object... args) {
		if (args.length > 0) {
			throw new IllegalArgumentException("Service instances factory does not support arguments.");
		}
		Class<?>[] interfaceClasses = managedClass.getInterfaceClasses();
		if (interfaceClasses == null) {
			throw new BugError("Invalid managed class. Null interface classes.");
		}
		if (interfaceClasses.length != 1) {
			throw new BugError("Invalid managed class. It should have exactly one interface class.");
		}
		return (I) Classes.loadService(interfaceClasses[0]);
	}
}
