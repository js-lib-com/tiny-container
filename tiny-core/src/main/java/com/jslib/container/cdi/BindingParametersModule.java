package com.jslib.container.cdi;

import java.util.ArrayList;
import java.util.List;

import com.jslib.api.injector.AbstractModule;
import com.jslib.api.injector.IBindingBuilder;

/**
 * Injector module for bindings declared via {@link BindingParameters binding parameters}. This module collects bindings
 * parameters and used them to create injector bindings when module configuration is executed.
 * 
 * @author Iulian Rotaru
 */
class BindingParametersModule extends AbstractModule {
	private final List<BindingParameters<?>> parametersList = new ArrayList<>();

	public void addBindingParameters(BindingParameters<?> parameters) {
		parametersList.add(parameters);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void configure() {
		parametersList.forEach(parameters -> {
			IBindingBuilder builder = bind(parameters.getInterfaceClass());
			if (parameters.getImplementationClass() != null) {
				builder.to(parameters.getImplementationClass());
			}
			if (parameters.getInstance() != null) {
				builder.instance(parameters.getInstance());
			}
			if (parameters.isService()) {
				builder.service();
			}
			if (parameters.getImplementationURL() != null) {
				builder.on(parameters.getImplementationURL());
			}
			if (parameters.getProvider() != null) {
				builder.provider(parameters.getProvider());
			}
			if (parameters.getScope() != null) {
				builder.in(parameters.getScope());
			}
		});
	}
}
