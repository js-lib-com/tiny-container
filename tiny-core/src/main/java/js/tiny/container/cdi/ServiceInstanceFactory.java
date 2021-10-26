package js.tiny.container.cdi;

import js.lang.NoProviderException;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.InstanceType;
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
	@Override
	public <I> I newInstance(IManagedClass<I> managedClass, Object... args) {
		if (args.length > 0) {
			throw new IllegalArgumentException("Service instances factory does not support arguments.");
		}
		return (I) Classes.loadService(managedClass.getInterfaceClass());
	}
}
