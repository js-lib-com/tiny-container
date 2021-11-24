package js.tiny.container.core;

import java.net.URI;

import js.tiny.container.cdi.Binding;
import js.tiny.container.cdi.CDI;
import js.tiny.container.spi.IBindingBuilder;
import js.tiny.container.spi.InstanceScope;
import js.tiny.container.spi.InstanceType;

public class BindingBuilder<T> implements IBindingBuilder<T> {
	private final CDI cdi;
	private final Class<T> interfaceClass;

	private Class<? extends T> implementationClass;
	private T instance;
	private InstanceType instanceType;
	private InstanceScope instanceScope;
	private URI implementationURL;

	public BindingBuilder(CDI cdi, Class<T> interfaceClass) {
		this.cdi = cdi;
		this.interfaceClass = interfaceClass;
	}

	@Override
	public IBindingBuilder<T> to(Class<? extends T> implementationClass) {
		this.implementationClass = implementationClass;
		return this;
	}

	@Override
	public IBindingBuilder<T> instance(T instance) {
		this.instance = instance;
		return this;
	}

	@Override
	public IBindingBuilder<T> type(InstanceType instanceType) {
		this.instanceType = instanceType;
		return this;
	}

	@Override
	public IBindingBuilder<T> scope(InstanceScope instanceScope) {
		this.instanceScope = instanceScope;
		return this;
	}

	@Override
	public IBindingBuilder<T> on(URI implementationURL) {
		this.implementationURL = implementationURL;
		return this;
	}

	@Override
	public void build() {
		if (instance != null) {
			cdi.bindInstance(interfaceClass, instance);
			return;
		}
		if (implementationClass == null) {
			implementationClass = interfaceClass;
		}
		if (instanceType == null) {
			instanceType = InstanceType.POJO;
		}
		if (instanceScope == null) {
			instanceScope = InstanceScope.APPLICATION;
		}
		Binding<T> binding = new Binding<>(interfaceClass, implementationClass, instanceType, instanceScope, implementationURL);
		cdi.bind(binding);
	}
}
