package js.tiny.container.cdi.impl;

import java.util.ArrayList;
import java.util.List;

import js.tiny.container.cdi.IBinding;
import js.tiny.container.cdi.IBindingBuilder;
import js.tiny.container.cdi.IModule;
import js.tiny.container.cdi.IProviders;

public abstract class AbstractModule implements IModule {
	private final List<IBinding<?>> bindings = new ArrayList<>();

	private IProviders providers;

	protected AbstractModule() {
	}

	@Override
	public void setScopeProviders(IProviders providers) {
		this.providers = providers;
	}

	protected <T> IBindingBuilder<T> bind(Class<T> type) {
		Binding<T> binding = new Binding<>(type, providers.getProvider(type));
		bindings.add(binding);
		return new BindingBuilder<>(providers, binding);
	}

	@Override
	public List<IBinding<?>> getBindings() {
		return bindings;
	}
}
