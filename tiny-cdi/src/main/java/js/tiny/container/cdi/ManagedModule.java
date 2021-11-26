package js.tiny.container.cdi;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.ManagedBean;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.inject.Provider;

import com.jslib.injector.IBinding;
import com.jslib.injector.IInjector;
import com.jslib.injector.IModule;
import com.jslib.injector.ITypedProvider;
import com.jslib.injector.ScopedProvider;

class ManagedModule implements IModule {

	private static final List<Class<? extends Annotation>> MANAGED_CLASS_ANNOTATIONS = new ArrayList<>();
	static {
		MANAGED_CLASS_ANNOTATIONS.add(ManagedBean.class);
		MANAGED_CLASS_ANNOTATIONS.add(Stateful.class);
		MANAGED_CLASS_ANNOTATIONS.add(Stateless.class);
	}

	private final IInjector injector;
	private final IManagedLoader managedLoader;
	private final List<Binding<?>> containerBindings = new ArrayList<>();

	private final List<IBinding<?>> bindings = new ArrayList<>();

	public ManagedModule(IInjector injector, IManagedLoader managedLoader) {
		this.injector = injector;
		this.managedLoader = managedLoader;
	}

	public void addModule(IModule module) {
		module.configure(injector);
		module.bindings().forEach(this::processBinding);
	}

	private <T> void processBinding(IBinding<T> binding) {
		Provider<T> provider = binding.provider();
		if (provider instanceof ScopedProvider) {
			provider = ((ScopedProvider<T>) provider).getProvisioningProvider();
		}
		if (!(provider instanceof ITypedProvider)) {
			bindings.add(binding);
			return;
		}

		Class<T> interfaceClass = binding.key().type();
		Class<? extends T> implementationClass = ((ITypedProvider<T>) provider).type();
		containerBindings.add(new Binding<>(interfaceClass, implementationClass));

		if (isAnnotationPresent(interfaceClass, implementationClass)) {
			System.out.println("--- managed bean: " + interfaceClass);
			bindings.add(ProxyBinding.create(managedLoader, binding));
		} else {
			bindings.add(binding);
		}
	}

	private static <T> boolean isAnnotationPresent(Class<T> interfaceClass, Class<? extends T> implementationClass) {
		for (Class<? extends Annotation> annotation : MANAGED_CLASS_ANNOTATIONS) {
			if (interfaceClass.isAnnotationPresent(annotation)) {
				return true;
			}
			if (implementationClass.isAnnotationPresent(annotation)) {
				return true;
			}
		}
		return false;
	}

	public List<Binding<?>> getContainerBindings() {
		return containerBindings;
	}

	@Override
	public IModule configure(IInjector injector) {
		return this;
	}

	@Override
	public List<IBinding<?>> bindings() {
		return bindings;
	}
}