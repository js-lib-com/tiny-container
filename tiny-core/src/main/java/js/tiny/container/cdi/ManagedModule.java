package js.tiny.container.cdi;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.ManagedBean;
import jakarta.ejb.Stateful;
import jakarta.ejb.Stateless;
import jakarta.inject.Provider;
import js.injector.IBinding;
import js.injector.IInjector;
import js.injector.IModule;
import js.injector.ITypedProvider;
import js.injector.ScopedProvider;

/**
 * Managed module collects all modules, from container and application, and pre-process them before handing to injector.
 * Resulting injector bindings are used to configure injector service. Beside injector bindings this module creates a list of
 * managed classes bindings used by container to actually create internal managed classes.
 * 
 * This module takes care to replace provider with {@link ProxyProvider} is given bound type has a {@link #PROXY_ANNOTATIONS}
 * annotation. Current implementation considers {@link ManagedBean}, {@link Stateful} and {@link Stateless} annotations for
 * proxy processing.
 * 
 * @author Iulian Rotaru
 */
class ManagedModule implements IModule {

	private static final List<Class<? extends Annotation>> PROXY_ANNOTATIONS = new ArrayList<>();
	static {
		PROXY_ANNOTATIONS.add(jakarta.annotation.ManagedBean.class);
		PROXY_ANNOTATIONS.add(jakarta.ejb.Stateful.class);
		PROXY_ANNOTATIONS.add(jakarta.ejb.Stateless.class);

		PROXY_ANNOTATIONS.add(javax.annotation.ManagedBean.class);
		PROXY_ANNOTATIONS.add(javax.ejb.Stateful.class);
		PROXY_ANNOTATIONS.add(javax.ejb.Stateless.class);
	}

	private final IInjector injector;
	private final IManagedLoader managedLoader;
	private final List<IClassBinding<?>> classBindings = new ArrayList<>();

	private final List<IBinding<?>> injectorBindings = new ArrayList<>();

	public ManagedModule(IInjector injector, IManagedLoader managedLoader) {
		this.injector = injector;
		this.managedLoader = managedLoader;
	}

	public void addModule(IModule module) {
		// module configure resolves module bindings but does not alter injector state
		module.configure(injector);
		module.bindings().forEach(this::processBinding);
	}

	private <T> void processBinding(IBinding<T> binding) {
		Provider<T> provider = binding.provider();
		if (provider instanceof ScopedProvider) {
			provider = ((ScopedProvider<T>) provider).getProvisioningProvider();
		}
		if (!(provider instanceof ITypedProvider)) {
			injectorBindings.add(binding);
			return;
		}

		Class<T> interfaceClass = binding.key().type();
		Class<? extends T> implementationClass = ((ITypedProvider<T>) provider).type();
		classBindings.add(new ClassBinding<>(interfaceClass, implementationClass));

		if (isProxyAnnotationPresent(interfaceClass, implementationClass)) {
			injectorBindings.add(ProxyBinding.create(managedLoader, binding));
		} else {
			injectorBindings.add(binding);
		}
	}

	private static <T> boolean isProxyAnnotationPresent(Class<T> interfaceClass, Class<? extends T> implementationClass) {
		for (Class<? extends Annotation> annotation : PROXY_ANNOTATIONS) {
			if (interfaceClass.isAnnotationPresent(annotation)) {
				return true;
			}
			if (implementationClass.isAnnotationPresent(annotation)) {
				return true;
			}
		}
		return false;
	}

	public List<IClassBinding<?>> getClassBindings() {
		return classBindings;
	}

	@Override
	public IModule configure(IInjector injector) {
		return this;
	}

	@Override
	public List<IBinding<?>> bindings() {
		return injectorBindings;
	}
}