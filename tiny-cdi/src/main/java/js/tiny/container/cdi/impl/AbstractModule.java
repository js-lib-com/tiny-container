package js.tiny.container.cdi.impl;

import java.util.ArrayList;
import java.util.List;

import js.tiny.container.cdi.IBinding;
import js.tiny.container.cdi.IBindingBuilder;
import js.tiny.container.cdi.IModule;
import js.tiny.container.cdi.service.CDI;

public abstract class AbstractModule implements IModule {
	private final List<IBinding<?>> bindings = new ArrayList<>();

	private CDI parentContainer;

	protected AbstractModule() {
	}

	@Override
	public IModule configure(CDI parentContainer) {
		this.parentContainer = parentContainer;
		configure();
		return this;
	}

	protected abstract void configure();

	protected <T> IBindingBuilder<T> bind(Class<T> type) {
		Binding<T> binding = new Binding<>(type, new ClassProvider<>(parentContainer, type));
		bindings.add(binding);
		return new BindingBuilder<>(parentContainer, binding);
	}

	@Override
	public List<IBinding<?>> bindings() {
		return bindings;
	}
}
