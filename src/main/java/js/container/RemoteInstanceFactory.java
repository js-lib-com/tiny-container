package js.container;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import js.core.AppFactory;
import js.net.client.HttpRmiFactory;
import js.rmi.RemoteFactory;
import js.rmi.RemoteFactoryProvider;
import js.rmi.UnsupportedProtocolException;
import js.util.Strings;

/**
 * Managed instances factory for classes deployed remotely. Beside being an instance factory this class also implements
 * {@link RemoteFactory}, providing support for {@link AppFactory#getRemoteInstance(String, Class)}.
 * <p>
 * <code>RemoteInstanceFactory</code> keeps a collection of {@link RemoteFactory} mapped by URL protocols. Current
 * implementation supports <code>HTTP-RMI</code> via {@link HttpRmiFactory}, mapped to both <code>http</code> and
 * <code>https</code>. This class also scans for remote factory extensions deployed on run-time context as standard Java
 * services, see {@link RemoteFactoryProvider}.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public class RemoteInstanceFactory implements InstanceFactory, RemoteFactory {
	/** Remote factories for specific URL protocols. */
	private Map<String, RemoteFactory> remoteFactories = new HashMap<>();

	/**
	 * Register built-in {@link RemoteFactory} for <code>HTTP-RMI</code> and scan for {@link RemoteFactoryProvider} extensions.
	 */
	public RemoteInstanceFactory() {
		RemoteFactory httpRemoteFactory = new HttpRmiFactory();
		remoteFactories.put("http", httpRemoteFactory);
		remoteFactories.put("https", httpRemoteFactory);

		for (RemoteFactoryProvider provider : ServiceLoader.load(RemoteFactoryProvider.class)) {
			remoteFactories.put(provider.getProtocol(), provider.getRemoteFactory());
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
	 * Also {@link ManagedClassSPI#getImplementationURL()} should be not null, that is, implementation URL should be present
	 * into class descriptor. Returned value is a Java proxy that delegates a HTTP-RMI client.
	 * <p>
	 * This factory method does not check managed class argument validity. It should be not null and configured for remote
	 * invocation.
	 * 
	 * @param managedClass managed class,
	 * @param args remote instance factory does not support constructor arguments.
	 * @param <T> managed instance type.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T newInstance(ManagedClassSPI managedClass, Object... args) {
		if (args.length > 0) {
			throw new IllegalArgumentException("REMOTE instance factory does not support arguments.");
		}
		return getRemoteInstance(managedClass.getImplementationURL(), (Class<? super T>) managedClass.getInterfaceClass());
	}

	/**
	 * Alternative to {@link #newInstance(ManagedClassSPI, Object...)} when implementation URL is obtained at run-time, perhaps
	 * from user interface. Returned value is a Java proxy that delegates a HTTP-RMI client. This method is designed
	 * specifically for {@link AppFactory#getRemoteInstance(String, Class)}.
	 * <p>
	 * This factory method does not check arguments validity. Both should be not null and interface class should be an actual
	 * Java interface.
	 * 
	 * @param implementationURL the URL of remote implementation,
	 * @param interfaceClass interface implemented by remote class.
	 * @param <T> managed class implementation.
	 * @return remote class proxy instance.
	 * @throws UnsupportedProtocolException if URL protocol is not supported or arguments are otherwise not valid or null - in
	 *             which case root cause has details about erroneous argument.
	 */
	@Override
	public <T> T getRemoteInstance(String implementationURL, Class<? super T> interfaceClass) throws UnsupportedProtocolException {
		if (implementationURL == null) {
			throw new UnsupportedProtocolException(new NullPointerException("Null remote implementation URL."));
		}
		String protocol = Strings.getProtocol(implementationURL);
		if (protocol == null) {
			throw new UnsupportedProtocolException(new MalformedURLException("Protocol not found on " + implementationURL));
		}

		RemoteFactory remoteFactory = remoteFactories.get(protocol);
		if (remoteFactory == null) {
			throw new UnsupportedProtocolException("No remote factory registered for protocol |%s|.", protocol);
		}
		return remoteFactory.getRemoteInstance(implementationURL, interfaceClass);
	}
}
