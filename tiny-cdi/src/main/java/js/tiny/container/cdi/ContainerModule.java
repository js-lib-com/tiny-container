package js.tiny.container.cdi;

import java.util.ArrayList;
import java.util.List;

import js.injector.AbstractModule;
import js.injector.IBindingBuilder;

public class ContainerModule extends AbstractModule {

	private final List<ContainerBinding<?>> bindings = new ArrayList<>();

	public void addBinding(ContainerBinding<?> binding) {
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
