package js.tiny.container.cdi;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;
import javax.inject.Singleton;

import com.jslib.injector.IBindingBuilder;
import com.jslib.injector.IInjector;
import com.jslib.injector.IModule;
import com.jslib.injector.IProvisionListener;
import com.jslib.injector.IScope;
import com.jslib.injector.Key;
import com.jslib.injector.ThreadScoped;
import com.jslib.injector.impl.AbstractModule;
import com.jslib.injector.impl.Injector;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.InstanceScope;
import js.tiny.container.spi.InstanceType;

public class CDI {
	private static final Log log = LogFactory.getLog(CDI.class);

	private static CDI instance;
	private static final Object mutex = new Object();

	public static CDI create(IModule... modules) {
		log.trace("create(Module...)");
		if (instance == null) {
			synchronized (mutex) {
				if (instance == null) {
					instance = new CDI();
				}
			}
		}
		return instance;
	}

	// --------------------------------------------------------------------------------------------

	private final IInjector injector;

	private CDI() {
		log.trace("CDI()");
		injector = new Injector();
	}

	public void configure(Collection<IManagedClass<?>> managedClasses) {
		injector.configure(new DescriptorModule(managedClasses));
	}

	public <T> void bindInstance(Class<T> interfaceClass, T instance) {
		injector.bindInstance(Key.get(interfaceClass), instance);
	}

	public void bindScope(Class<? extends Annotation> annotation, IScope scope) {
		injector.bindScope(annotation, scope);
	}

	public <T> T getScopeInstance(Class<T> type) {
		return injector.getScopeInstance(type);
	}

	private final Map<Provider<?>, IManagedClass<?>> providedClasses = new HashMap<>();

	@SuppressWarnings("unchecked")
	public <T> T getInstance(Class<T> type, IInstancePostConstructionListener<T> instanceListener) {
		IProvisionListener<T> provisionListener = invocation -> {
			instanceListener.onInstancePostConstruction((IManagedClass<T>) providedClasses.get(invocation.provider()), (T) invocation.instance());
		};
		injector.bindListener(provisionListener);
		try {
			return injector.getInstance(Key.get(type));
		} finally {
			injector.unbindListener(provisionListener);
		}
	}

	public void clear() {
		// TODO Auto-generated method stub

	}

	private class DescriptorModule extends AbstractModule {
		private final Collection<IManagedClass<?>> managedClasses;

		public DescriptorModule(Collection<IManagedClass<?>> managedClasses) {
			this.managedClasses = managedClasses;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		protected void configure() {
			managedClasses.forEach(managedClass -> {
				log.debug("CDI register managed class |%s|.", managedClass);

				IBindingBuilder bindingBuilder = bind(managedClass.getInterfaceClass());

				final InstanceType instanceType = managedClass.getInstanceType();
				if (instanceType.isPOJO()) {
					bindingBuilder.to(managedClass.getImplementationClass());
					providedClasses.put(bindingBuilder.getProvider(), managedClass);
				} else if (instanceType.isPROXY()) {
					bindingBuilder.to(managedClass.getImplementationClass());
					providedClasses.put(bindingBuilder.getProvider(), managedClass);
					bindingBuilder.toProvider(new ProxyProvider(managedClass, bindingBuilder.getProvider()));
				} else if (instanceType.isREMOTE()) {
					bindingBuilder.on(managedClass.getImplementationURL());
				} else if (instanceType.isSERVICE()) {
					bindingBuilder.toProvider(new ServiceProvider<>(injector, managedClass.getInterfaceClass()));
					providedClasses.put(bindingBuilder.getProvider(), managedClass);
				} else {
					throw new IllegalStateException("No provider for instance type " + instanceType);
				}

				final InstanceScope instanceScope = managedClass.getInstanceScope();
				if (instanceScope.isLOCAL()) {
					// local scope always creates a new instance
				} else if (instanceScope.isAPPLICATION()) {
					bindingBuilder.in(Singleton.class);
				} else if (instanceScope.isTHREAD()) {
					bindingBuilder.in(ThreadScoped.class);
				} else if (instanceScope.isSESSION()) {
					bindingBuilder.in(SessionScoped.class);
				} else {
					throw new IllegalStateException("No provider for instance scope " + instanceScope);
				}
			});
		}
	}
}
