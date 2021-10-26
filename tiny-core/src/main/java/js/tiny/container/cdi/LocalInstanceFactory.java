package js.tiny.container.cdi;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

import js.lang.BugError;
import js.lang.InvocationException;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.core.ManagedProxyHandler;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.InstanceType;
import js.util.Classes;
import js.util.Strings;

/**
 * Built-in factory for local instances, local by contrast with remote or service instances. This instances factory deals with
 * {@link InstanceType#POJO} and {@link InstanceType#PROXY} managed classes. They are both created by invoking constructor with
 * provided arguments, that should be in the order and of types requested by constructor signature. There is no attempt to
 * discover constructor at runtime based on given arguments; this factory uses constructor provided by managed class, see
 * {@link IManagedClass#getConstructor()}.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public class LocalInstanceFactory implements InstanceFactory {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(LocalInstanceFactory.class);

	/**
	 * Local instances factory is built-in and does not have a type name.
	 * 
	 * @throws UnsupportedOperationException this operation is not supported.
	 */
	@Override
	public InstanceType getInstanceType() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Uses managed class constructor to create new instance with provided arguments. Arguments should be in order, types and
	 * number required by constructor signature.
	 * 
	 * @param managedClass managed class,
	 * @param args optional arguments as required by constructor signature.
	 * @param <T> instance type;
	 * @return newly created managed instance.
	 * @throws IllegalArgumentException if <code>args</code> argument is not of types and number required by constructor.
	 * @throws InvocationException if constructor execution fails.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T newInstance(IManagedClass<T> managedClass, Object... args) {
		Constructor<? extends T> constructor = managedClass.getConstructor();
		if (constructor == null) {
			throw new BugError("Local instance factory cannot instantiate |%s|. Missing constructor.", managedClass);
		}

		T instance = null;
		try {
			instance = constructor.newInstance(args);
		} catch (IllegalArgumentException e) {
			log.error("Wrong number of arguments or bad types for |%s|: [%s].", constructor, Strings.join(Classes.getParameterTypes(args)));
			throw e;
		} catch (InstantiationException e) {
			// managed class implementation is already validated, i.e. is not abstract and
			// test for existing constructor is performed... so no obvious reasons for instantiation exception
			throw new BugError(e);
		} catch (IllegalAccessException e) {
			// constructor has accessibility true and class is tested for public access modifier
			// so there is no reason for illegal access exception
			throw new BugError(e);
		} catch (InvocationTargetException e) {
			log.error("Managed instance constructor |%s| fail due to: %s.", constructor, e.getCause());
			throw new InvocationException(e);
		}

		// TODO: is quite possible to get rid of proxy from container core
		if (managedClass.getInstanceType().equals(InstanceType.PROXY)) {
			final ClassLoader classLoader = managedClass.getImplementationClass().getClassLoader();
			final Class<?>[] interfaceClasses = new Class[] { managedClass.getInterfaceClass() };
			final ManagedProxyHandler<T> handler = new ManagedProxyHandler<T>(managedClass, instance);
			return (T) Proxy.newProxyInstance(classLoader, interfaceClasses, handler);
		}

		return instance;
	}
}
