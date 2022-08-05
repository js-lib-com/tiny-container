package com.jslib.tiny.container.cdi;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.api.injector.IBinding;
import com.jslib.api.injector.IInjector;
import com.jslib.api.injector.IModule;
import com.jslib.api.injector.ITypedProvider;
import com.jslib.api.injector.Key;

import jakarta.inject.Provider;

@RunWith(MockitoJUnitRunner.class)
public class ManagedModuleTest {
	@Mock
	private IInjector injector;
	@Mock
	private IManagedLoader managedLoader;
	@Mock
	private IModule module;
	@Mock
	private IBinding<?> binding;
	@Mock
	private Key<?> key;
	@Mock
	private Provider<?> provider;
	@Mock
	private ITypedProvider<?> typedProvider;

	private ManagedModule managedModule;

	@Before
	public void beforeTest() {
		when(module.bindings()).thenReturn(Arrays.asList(binding));
		doReturn(key).when(binding).key();
		doReturn(Object.class).when(key).type();
		doReturn(Object.class).when(typedProvider).type();
		
		managedModule = new ManagedModule(injector, managedLoader);
	}

	@Test
	public void GivenTypeProvider_WhenAddModule_Then() {
		// given
		doReturn(typedProvider).when(binding).provider();
		
		// when
		managedModule.addModule(module);

		// then
	}

	@Test
	public void GivenStandardProvider_WhenAddModule_Then() {
		// given
		doReturn(provider).when(binding).provider();
		
		// when
		managedModule.addModule(module);

		// then
	}

	@Test
	public void Given_When_Then() {
		// given

		// when

		// then
	}
}
