package js.tiny.container.cdi;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import js.lang.BugError;
import js.rmi.RemoteFactory;
import js.rmi.RemoteFactoryProvider;
import js.rmi.UnsupportedProtocolException;
import js.tiny.container.spi.IFactory;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.InstanceType;
import js.util.Strings;

/**
 * Managed instances factory for classes deployed remotely. Beside being an instance factory this class also implements
 * {@link RemoteFactory}, providing support for {@link IFactory#getRemoteInstance(String, Class)}.
 * <p>
 * <code>RemoteInstanceFactory</code> keeps a collection of {@link RemoteFactory} mapped by URL protocols. This class scans for
 * remote factory extensions deployed on run-time context as standard Java services, see {@link RemoteFactoryProvider}. It is
 * not legal to override an already registered protocol.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public class RemoteInstanceFactory implements InstanceFactory, RemoteFactory {
	/** Remote factories for specific URL protocols. */
	private Map<String, RemoteFactory> remoteFactories = new HashMap<>();

	/**
	 * Scan for {@link RemoteFactoryProvider} extensions and register {@link RemoteFactory} for provided protocols. Protocol
	 * syntax is implementation detail but is part of <code>implementationURL</code> supplied - as declared by provider, to
	 * {@link #getRemoteInstance(String, Class)}.
	 * 
	 * @throws BugError if attempt to override already registered protocol.
	 */
	public RemoteInstanceFactory() {
		for (RemoteFactoryProvider provider : ServiceLoader.load(RemoteFactoryProvider.class)) {
			for (String protocol : provider.getProtocols()) {
				if (remoteFactories.put(protocol, provider.getRemoteFactory()) != null) {
					throw new BugError("Invalid runtime environment. Remote factory protocol override |%s|. See remote factory provider |%s|.", protocol, provider);
				}
			}
		}
	}

	/**
	 * Returns {@link InstanceType#REMOTE}, the instance type this factory instance is bound to.
	 * 
	 * @return instance type this factory is bound to.
	 */
	@Override
	public InstanceType getInstanceType() {
		return InstanceType.REMOTE;
	}

	/**
	 * Create a new instance for given remote managed class. Managed class should be declared as remote into class descriptor.
	 * Also {@link IManagedClass#getImplementationURL()} should be not null, that is, implementation URL should be present
	 * into class descriptor. Returned value is a Java proxy that delegates a HTTP-RMI client.
	 * <p>
	 * This factory method does not check managed class argument validity. It should be not null and configured for remote
	 * invocation.
	 * 
	 * @param managedClass managed class,
	 * @param args remote instance factory does not support constructor arguments.
	 * @param <T> managed instance type.
	 */
	@Override
	public <T> T newInstance(IManagedClass<T> managedClass, Object... args) {
		if (args.length > 0) {
			throw new IllegalArgumentException("REMOTE instance factory does not support arguments.");
		}
		return getRemoteInstance((Class<? super T>) managedClass.getInterfaceClass(), managedClass.getImplementationURL());
	}

	/**
	 * Alternative to {@link #newInstance(IManagedClass, Object...)} when implementation URL is obtained at run-time, perhaps
	 * from user interface. Returned value is a Java proxy that delegates a HTTP-RMI client. This method is designed
	 * specifically for {@link IFactory#getRemoteInstance(String, Class)}.
	 * <p>
	 * This factory method does not check arguments validity. Both should be not null and interface class should be an actual
	 * Java interface.
	 * 
	 * @param interfaceClass interface implemented by remote class,
	 * @param implementationURL the URL of remote implementation.
	 * @param <T> managed class implementation.
	 * @return remote class proxy instance.
	 * @throws UnsupportedProtocolException if URL protocol is not supported or arguments are otherwise not valid or null - in
	 *             which case root cause has details about erroneous argument.
	 */
	@Override
	public <T> T getRemoteInstance(Class<? super T> interfaceClass, String implementationURL) throws UnsupportedProtocolException {
		if (implementationURL == null) {
			throw new UnsupportedProtocolException(new NullPointerException("Null remote implementation URL."));
		}
		String protocol = Strings.getProtocol(implementationURL);
		if (protocol == null) {
			throw new UnsupportedProtocolException(new MalformedURLException("Protocol not found on " + implementationURL));
		}

		RemoteFactory remoteFactory = remoteFactories.get(protocol);
		if (remoteFactory == null) {
			throw new UnsupportedProtocolException("No service provider for remote factory on protocol |%s|. Ensure service provider for |%s| interface is deployed on runtime libraries.", protocol, RemoteFactoryProvider.class);
		}
		return remoteFactory.getRemoteInstance(interfaceClass, implementationURL);
	}
}
