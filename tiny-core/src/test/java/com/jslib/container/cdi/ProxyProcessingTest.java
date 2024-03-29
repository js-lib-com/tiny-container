package com.jslib.container.cdi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.api.injector.IBinding;
import com.jslib.api.injector.IModule;
import com.jslib.api.injector.ITypedProvider;
import com.jslib.api.injector.Key;
import com.jslib.api.injector.ScopedProvider;

import jakarta.annotation.ManagedBean;
import jakarta.ejb.Stateful;
import jakarta.ejb.Stateless;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

@RunWith(MockitoJUnitRunner.class)
public class ProxyProcessingTest {
	@Mock
	private IModule module;
	@Mock
	private IManagedLoader loader;

	private List<IBinding<?>> bindings;

	private CDI cdi;

	@Before
	public void beforeTest() {
		bindings = new ArrayList<>();
		when(module.bindings()).thenReturn(bindings);

		cdi = CDI.create();
		cdi.setManagedLoader(loader);
	}

	@Test
	public void GivenNoProxyMarker_WhenGetInstance_ThenInstanceNotProxy() throws InstantiationException, IllegalAccessException {
		// given
		bindings.add(new Binding<>(Service.class));
		cdi.configure(module);

		// when
		IService instance = cdi.getInstance(IService.class);

		// then
		assertThat(instance, notNullValue());
		assertThat(Proxy.isProxyClass(instance.getClass()), is(false));
	}

	@Test
	public void GivenManagedBeanInterface_WhenGetInstance_ThenInstanceProxy() throws InstantiationException, IllegalAccessException {
		// given
		bindings.add(new Binding<>(ManagedBean1.class));
		cdi.configure(module);

		// when
		IManagedBean1 instance = cdi.getInstance(IManagedBean1.class);

		// then
		assertThat(instance, notNullValue());
		assertThat(Proxy.isProxyClass(instance.getClass()), is(true));
	}

	@Test
	public void GivenManagedBeanClass_WhenGetInstance_ThenInstanceProxy() throws InstantiationException, IllegalAccessException {
		// given
		bindings.add(new Binding<>(ManagedBean2.class));
		cdi.configure(module);

		// when
		IManagedBean2 instance = cdi.getInstance(IManagedBean2.class);

		// then
		assertThat(instance, notNullValue());
		assertThat(Proxy.isProxyClass(instance.getClass()), is(true));
	}

	/**
	 * Given interface annotated with singleton scope created instance is not a proxy since created proxy provider is wrapped by
	 * scope provider.
	 */
	@Test
	public void GivenScopedManagedBeanInterface_WhenGetInstance_ThenInstanceNotProxy() throws InstantiationException, IllegalAccessException {
		// given
		bindings.add(new Binding<>(ManagedBean3.class));
		cdi.configure(module);

		// when
		IManagedBean3 instance = cdi.getInstance(IManagedBean3.class);

		// then
		assertThat(instance, notNullValue());
		assertThat(Proxy.isProxyClass(instance.getClass()), is(false));
	}

	// --------------------------------------------------------------------------------------------

	public static interface IService {

	}

	public static class Service implements IService {

	}

	@ManagedBean
	public static interface IManagedBean1 {

	}

	public static class ManagedBean1 implements IManagedBean1 {

	}

	public static interface IManagedBean2 {

	}

	@ManagedBean
	public static class ManagedBean2 implements IManagedBean2 {

	}

	@ManagedBean
	@Singleton
	public static interface IManagedBean3 {

	}

	public static class ManagedBean3 implements IManagedBean3 {

	}

	@Stateful
	public static interface IStateful1 {

	}

	public static class Stateful1 implements IStateful1 {

	}

	public static interface IStateful2 {

	}

	@Stateful
	public static class Stateful2 implements IStateful2 {

	}

	@Stateless
	public static interface IStateless1 {

	}

	public static class Stateless1 implements IStateless1 {

	}

	public static interface IStateless2 {

	}

	@Stateless
	public static class Stateless2 implements IStateless2 {

	}

	private static class Binding<T> implements IBinding<T> {
		private final Key<T> key;
		private final Provider<T> provider;

		@SuppressWarnings("unchecked")
		public Binding(Class<? extends T> implementationClass) throws InstantiationException, IllegalAccessException {
			Class<T> interfaceClass = (Class<T>) implementationClass.getInterfaces()[0];
			T instance;
			try {
				instance = implementationClass.getConstructor().newInstance();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
				throw new IllegalStateException(e);
			}

			Provider<T> provider = new ITypedProvider<T>() {
				@Override
				public Class<? extends T> type() {
					return implementationClass;
				}

				@Override
				public T get() {
					return instance;
				}
			};

			if (interfaceClass.isAnnotationPresent(Singleton.class)) {
				provider = new ScopedProvider<T>(provider) {
					@Override
					public T get() {
						return instance;
					}

					@Override
					public Class<? extends Annotation> getScope() {
						return Singleton.class;
					}

					@Override
					public T getScopeInstance() {
						return null;
					}
				};
			}

			this.key = Key.get(interfaceClass);
			this.provider = provider;
		}

		@Override
		public Key<T> key() {
			return key;
		}

		@Override
		public Provider<T> provider() {
			return provider;
		}
	}
}
