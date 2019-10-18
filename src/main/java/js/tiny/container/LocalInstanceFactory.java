package js.tiny.container;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

import js.lang.BugError;
import js.lang.InvocationException;
import js.log.Log;
import js.log.LogFactory;
import js.util.Classes;
import js.util.Strings;

/**
 * Built-in factory for local instances, local by contrast with remote or service instances. This instances factory deals with
 * {@link InstanceType#POJO} and {@link InstanceType#PROXY} managed classes. They are both created by invoking constructor with
 * provided arguments, that should be in the order and of types requested by constructor signature. There is no attempt to
 * discover constructor at runtime based on given arguments; this factory uses constructor provided by managed class, see
 * {@link ManagedClassSPI#getConstructor()}.
 * 
 * @author Iulian Rotaru
 * @version final
 */
final class LocalInstanceFactory implements InstanceFactory {
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
	public <T> T newInstance(ManagedClassSPI managedClass, Object... args) {
		Constructor<?> constructor = managedClass.getConstructor();
		if (constructor == null) {
			throw new BugError("Local instance factory cannot instantiate |%s|. Missing constructor.", managedClass);
		}

		Object instance = null;
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

		if (managedClass.getInstanceType().equals(InstanceType.PROXY)) {
			// there are two proxy handlers: one transactional and one not
			// the difference is that transactional proxy handler gets a reference to an external transactional resource

			ManagedProxyHandler handler = null;
			if (managedClass.isTransactional()) {
				TransactionalResource transactionalResource = managedClass.getContainer().getInstance(TransactionalResource.class);
				handler = new ManagedProxyHandler(transactionalResource, managedClass, instance);
			} else {
				handler = new ManagedProxyHandler(managedClass, instance);
			}

			final ClassLoader classLoader = managedClass.getImplementationClass().getClassLoader();
			final Class<?>[] interfaceClasses = managedClass.getInterfaceClasses();
			return (T) Proxy.newProxyInstance(classLoader, interfaceClasses, handler);
		}

		return (T) instance;
	}
}
