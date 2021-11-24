package js.tiny.container.cdi;

import java.net.URI;

import js.lang.Config;
import js.tiny.container.spi.InstanceScope;
import js.tiny.container.spi.InstanceType;

public class Binding<T> {
	private final Class<T> interfaceClass;
	private final Class<? extends T> implementationClass;

	private final InstanceType instanceType;
	private final InstanceScope instanceScope;
	private final URI implementationURL;

	@SuppressWarnings("unchecked")
	public Binding(Config config) {
		this.implementationClass = config.getAttribute("class", Class.class);
		this.interfaceClass = config.getAttribute("interface", Class.class, this.implementationClass);
		this.instanceType = config.getAttribute("type", InstanceType.class, InstanceType.POJO);
		this.instanceScope = config.getAttribute("scope", InstanceScope.class, InstanceScope.APPLICATION);
		this.implementationURL = config.getAttribute("url", URI.class);
	}

	public Binding(Class<T> interfaceClass, Class<? extends T> implementationClass) {
		this.interfaceClass = interfaceClass;
		this.implementationClass = implementationClass;
		this.instanceType = null;
		this.instanceScope = null;
		this.implementationURL = null;
	}

	public Binding(Class<T> interfaceClass, Class<? extends T> implementationClass, InstanceType instanceType, InstanceScope instanceScope, URI implementationURL) {
		this.interfaceClass = interfaceClass;
		this.implementationClass = implementationClass;
		this.instanceType = instanceType;
		this.instanceScope = instanceScope;
		this.implementationURL = implementationURL;
	}

	/**
	 * Test constructor. Initialize both interface and implementation from given Java type, instance type to
	 * {@link InstanceType#POJO} and instance scope to {@link InstanceScope#APPLICATION}; implementation URL is null.
	 * 
	 * @param type binding type used for both interface and implementation.
	 */
	public Binding(Class<T> type) {
		this.interfaceClass = type;
		this.implementationClass = type;
		this.instanceType = InstanceType.POJO;
		this.instanceScope = InstanceScope.APPLICATION;
		this.implementationURL = null;
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