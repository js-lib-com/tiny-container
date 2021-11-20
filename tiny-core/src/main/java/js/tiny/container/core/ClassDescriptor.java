package js.tiny.container.core;

import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;

import js.lang.Config;
import js.lang.ConfigException;
import js.tiny.container.spi.IClassDescriptor;
import js.tiny.container.spi.InstanceScope;
import js.tiny.container.spi.InstanceType;
import js.util.Classes;
import js.util.Types;

public class ClassDescriptor<T> implements IClassDescriptor<T> {
	private static final AtomicInteger keyGenerator = new AtomicInteger();

	private final String managedClassKey;
	private final Class<T> interfaceClass;
	private final Class<? extends T> implementationClass;
	private final InstanceType instanceType;
	private final InstanceScope instanceScope;
	private final String implementationURL;

	public ClassDescriptor(Class<T> interfaceClass) {
		this.managedClassKey = Integer.toString(keyGenerator.getAndIncrement());
		this.interfaceClass = interfaceClass;
		this.implementationClass = interfaceClass;
		this.instanceType = InstanceType.POJO;
		this.instanceScope = InstanceScope.APPLICATION;
		this.implementationURL = null;
	}

	public ClassDescriptor(Config descriptor) throws ConfigException {
		this.managedClassKey = Integer.toString(keyGenerator.getAndIncrement());
		this.instanceScope = descriptor.getAttribute("scope", InstanceScope.class, InstanceScope.APPLICATION);
		this.instanceType = descriptor.getAttribute("type", InstanceType.class, InstanceType.POJO, ConfigException.class);

		this.implementationClass = loadImplementationClass(descriptor);
		this.interfaceClass = loadInterfaceClass(descriptor);
		this.implementationURL = loadImplementationURL(descriptor);
	}

	@SuppressWarnings("unchecked")
	private Class<T> loadInterfaceClass(Config descriptor) throws ConfigException {
		if (!descriptor.hasAttribute("interface")) {
			if (instanceType.requiresInterface()) {
				throw new ConfigException("Managed type |%s| requires <interface> attribute. See class descriptor |%s|.", instanceType, descriptor);
			}
			// if interface is not required and is missing uses implementation class
			return (Class<T>) implementationClass;
		}

		if ("REMOTE".equals(descriptor.getAttribute("type"))) {
			String url = descriptor.getAttribute("url");
			if (url == null || url.isEmpty()) {
				throw new ConfigException("Managed type REMOTE requires <url> attribute. See class descriptor |%s|.", descriptor);
			}
			if (url.startsWith("${")) {
				throw new ConfigException("Remote implementation <url> property not resolved. See class descriptor |%s|.", descriptor);
			}
		}

		String interfaceName = descriptor.getAttribute("interface");
		final Class<T> interfaceClass = Classes.forOptionalName(interfaceName);

		if (interfaceClass == null) {
			throw new ConfigException("Managed class interface |%s| not found.", interfaceName);
		}
		if (instanceType.requiresInterface() && !interfaceClass.isInterface()) {
			throw new ConfigException("Managed type |%s| requires interface to make Java Proxy happy but got |%s|.", instanceType, interfaceClass);
		}
		if (implementationClass != null && !Types.isKindOf(implementationClass, interfaceClass)) {
			throw new ConfigException("Implementation |%s| is not a kind of interface |%s|.", implementationClass, interfaceClass);
		}

		return interfaceClass;
	}

	private Class<? extends T> loadImplementationClass(Config descriptor) throws ConfigException {
		String implementationName = descriptor.getAttribute("class");
		if (implementationName == null) {
			if (instanceType.requiresImplementation()) {
				throw new ConfigException("Managed type |%s| requires <class> attribute. See class descriptor |%s|.", instanceType, descriptor);
			}
			return null;
		}
		if (!instanceType.requiresImplementation()) {
			throw new ConfigException("Managed type |%s| forbids <class> attribute. See class descriptor |%s|.", instanceType, descriptor);
		}

		Class<? extends T> implementationClass = Classes.forOptionalName(implementationName);
		if (implementationClass == null) {
			throw new ConfigException("Managed class implementation |%s| not found.", implementationName);
		}

		if (implementationClass.isInterface()) {
			throw new ConfigException("Managed class implementation |%s| cannot be an interface. See class descriptor |%s|.", implementationClass, descriptor);
		}
		int implementationModifiers = implementationClass.getModifiers();
		if (Modifier.isAbstract(implementationModifiers)) {
			throw new ConfigException("Managed class implementation |%s| cannot be abstract. See class descriptor |%s|.", implementationClass, descriptor);
		}

		return implementationClass;
	}

	private String loadImplementationURL(Config descriptor) throws ConfigException {
		String implementationURL = descriptor.getAttribute("url");
		if (instanceType.equals(InstanceType.REMOTE) && implementationURL == null) {
			throw new ConfigException("Remote managed class requires <url> attribute. See class descriptor |%s|.", descriptor);
		}
		return implementationURL;
	}

	public String getManagedClassKey() {
		return managedClassKey;
	}

	@Override
	public Class<T> getInterfaceClass() {
		return interfaceClass;
	}

	@Override
	public Class<? extends T> getImplementationClass() {
		return implementationClass;
	}

	@Override
	public InstanceType getInstanceType() {
		return instanceType;
	}

	@Override
	public InstanceScope getInstanceScope() {
		return instanceScope;
	}

	@Override
	public String getImplementationURL() {
		return implementationURL;
	}
}
