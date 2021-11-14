package js.tiny.container.spi;

import java.util.concurrent.atomic.AtomicInteger;

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
