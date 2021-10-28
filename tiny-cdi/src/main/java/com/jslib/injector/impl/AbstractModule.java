package com.jslib.injector.impl;

import java.util.ArrayList;
import java.util.List;

import com.jslib.injector.IBinding;
import com.jslib.injector.IBindingBuilder;
import com.jslib.injector.IInjector;
import com.jslib.injector.IModule;

public abstract class AbstractModule implements IModule {
	private final List<IBinding<?>> bindings = new ArrayList<>();

	private IInjector injector;

	protected AbstractModule() {
	}

	@Override
	public IModule configure(IInjector injector) {
		this.injector = injector;
		configure();
		return this;
	}

	protected abstract void configure();

	/**
	 * Add new binding to internal bindings list and start chained builder. Caller may want to invoke various mutators on
	 * returned builder in order to configure newly created binding.
	 * 
	 * If <code>type</code> argument is an interface caller should invoke {@link IBindingBuilder#to(Class)} in order to
	 * configure provider implementation for newly created binding. If <code>type</code> argument is concrete a default provider
	 * is created and is not mandatory for caller to invoke {@link IBindingBuilder#to(Class)}; anyway, is not forbidden.
	 * 
	 * @param type desired instance type.
	 * @return chained builder for newly created binding.
	 * @param <T> class parameter type.
	 */
	protected <T> IBindingBuilder<T> bind(Class<T> type) {
		Binding<T> binding = type.isInterface() ? new Binding<>(type) : new Binding<>(type, new ClassProvider<>(injector, type));
		bindings.add(binding);
		return new BindingBuilder<>(injector, binding);
	}

	@Override
	public List<IBinding<?>> bindings() {
		return bindings;
	}
}
