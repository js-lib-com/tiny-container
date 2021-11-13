package js.tiny.container.spi;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

public class ClassDescriptor<T> {
	private static final AtomicInteger keyGenerator = new AtomicInteger();

	private final String managedClassKey;
	private final Class<T> interfaceClass;
	private final Class<? extends T> implementationClass;
	private final InstanceType instanceType;
	private final InstanceScope instanceScope;
	private final URI implementationURL;

	public ClassDescriptor(Class<T> interfaceClass) {
		this.managedClassKey = Integer.toString(keyGenerator.getAndIncrement());
		this.interfaceClass = interfaceClass;
		this.implementationClass = interfaceClass;
		this.instanceType = InstanceType.POJO;
		this.instanceScope = InstanceScope.APPLICATION;
		this.implementationURL = null;
	}

	public String getManagedClassKey() {
		return managedClassKey;
	}

	public Class<T> getInterfaceClass() {
		return interfaceClass;
	}

	public Class<? extends T> getImplementationClass() {
		return implementationClass;
	}

	public InstanceType getInstanceType() {
		return instanceType;
	}

	public InstanceScope getInstanceScope() {
		return instanceScope;
	}

	public URI getImplementationURL() {
		return implementationURL;
	}
}
