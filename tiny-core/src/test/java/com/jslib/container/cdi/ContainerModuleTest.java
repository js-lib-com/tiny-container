package com.jslib.container.cdi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.api.injector.IBindingBuilder;
import com.jslib.api.injector.IInjector;

import jakarta.inject.Singleton;

@RunWith(MockitoJUnitRunner.class)
public class ContainerModuleTest {
	@Mock
	private IInjector injector;
	@Mock
	private IBindingBuilder<?> bindingBuilder;

	private BindingParametersModule module;

	@Before
	public void beforeTest() {
		module = new BindingParametersModule();
	}

	@Test
	public void GivenOnlyInterface_WhenConfigure_ThenDefaultBinding() {
		// given
		doReturn(bindingBuilder).when(injector).getBindingBuilder(Service.class);
		module.addBindingParameters(new BindingParameters<>(Service.class));

		// when
		module.configure(injector).bindings();

		// then
		verify(bindingBuilder, times(0)).to(any());
		verify(bindingBuilder, times(0)).instance(any());
		verify(bindingBuilder, times(0)).service();
		verify(bindingBuilder, times(0)).in(any());
	}

	@Test
	public void GivenImplementation_WhenConfigure_ThenBindingTo() {
		// given
		doReturn(bindingBuilder).when(injector).getBindingBuilder(IService.class);
		module.addBindingParameters(new BindingParameters<>(IService.class).setImplementationClass(Service.class));

		// when
		module.configure(injector).bindings();

		// then
		verify(bindingBuilder, times(1)).to(any());
		verify(bindingBuilder, times(0)).instance(any());
		verify(bindingBuilder, times(0)).service();
		verify(bindingBuilder, times(0)).in(any());
	}

	@Test
	public void GivenInstance_WhenConfigure_ThenBindingInstance() {
		// given
		doReturn(bindingBuilder).when(injector).getBindingBuilder(IService.class);
		module.addBindingParameters(new BindingParameters<>(IService.class).setInstance(new Service()));

		// when
		module.configure(injector).bindings();

		// then
		verify(bindingBuilder, times(0)).to(any());
		verify(bindingBuilder, times(1)).instance(any());
		verify(bindingBuilder, times(0)).service();
		verify(bindingBuilder, times(0)).in(any());
	}

	@Test
	public void GivenService_WhenConfigure_ThenBindingService() {
		// given
		doReturn(bindingBuilder).when(injector).getBindingBuilder(IService.class);
		module.addBindingParameters(new BindingParameters<>(IService.class).setService(true));

		// when
		module.configure(injector).bindings();

		// then
		verify(bindingBuilder, times(0)).to(any());
		verify(bindingBuilder, times(0)).instance(any());
		verify(bindingBuilder, times(1)).service();
		verify(bindingBuilder, times(0)).in(any());
	}

	@Test
	public void GivenScope_WhenConfigure_ThenBindingService() {
		// given
		doReturn(bindingBuilder).when(injector).getBindingBuilder(IService.class);
		module.addBindingParameters(new BindingParameters<>(IService.class).setScope(Singleton.class));

		// when
		module.configure(injector).bindings();

		// then
		verify(bindingBuilder, times(0)).to(any());
		verify(bindingBuilder, times(0)).instance(any());
		verify(bindingBuilder, times(0)).service();
		verify(bindingBuilder, times(1)).in(any());
	}
	
	// --------------------------------------------------------------------------------------------
	
	private interface IService {
	}

	private static class Service implements IService {
	}
}
