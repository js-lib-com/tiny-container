package js.tiny.container.cdi;

import java.util.ArrayList;
import java.util.List;

import js.injector.AbstractModule;
import js.injector.IBindingBuilder;

/**
 * Injector module that handle container internal bindings. This module collects bindings parameters and used them to create
 * injector bindings when module configuration is executed.
 * 
 * @author Iulian Rotaru
 */
class ContainerModule extends AbstractModule {
	private final List<ContainerBindingParameters<?>> bindings = new ArrayList<>();

	public void addBinding(ContainerBindingParameters<?> binding) {
		bindings.add(binding);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void configure() {
		bindings.forEach(binding -> {
			IBindingBuilder builder = bind(binding.getInterfaceClass());
			if (binding.getImplementationClass() != null) {
				builder.to(binding.getImplementationClass());
			}
			if (binding.getInstance() != null) {
				builder.instance(binding.getInstance());
			}
			if (binding.isService()) {
				builder.service();
			}
			if (binding.getScope() != null) {
				builder.in(binding.getScope());
			}
		});
	}
}
