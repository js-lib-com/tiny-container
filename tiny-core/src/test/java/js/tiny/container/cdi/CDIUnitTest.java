package js.tiny.container.cdi;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.URI;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import js.injector.IBinding;
import js.injector.IInjector;
import js.injector.IScopeFactory;
import js.injector.ProvisionException;
import js.injector.ThreadScoped;
import js.tiny.container.fixture.IService;
import js.tiny.container.fixture.Service;
import js.tiny.container.fixture.ThreadData;
import js.tiny.container.spi.IInstanceLifecycleListener;

@RunWith(MockitoJUnitRunner.class)
public class CDIUnitTest {
	@Mock
	private IInstanceLifecycleListener instanceListener;
	@Mock
	private BindingParameters<?> bindingParameters;

	private CDI cdi;

	@Before
	public void beforeTest() {
		doReturn(Object.class).when(bindingParameters).getInterfaceClass();
		doReturn(Object.class).when(bindingParameters).getImplementationClass();

		cdi = CDI.create();
		cdi.setInstanceCreatedListener(instanceListener);
		cdi.bind(bindingParameters);
	}

	@Test
	public void GivenTypeLOCAL_WhenGetInstance_ThenCreateNew() {
		// given
		cdi.configure();

		// when
		Object instance = cdi.getInstance(Object.class);

		// then
		assertThat(instance, notNullValue());
		verify(instanceListener, times(1)).onInstanceCreated(instance);
	}

	@Test
	public void GivenTypePROXY_WhenGetInstance_ThenCreateNew() {
		// given
		doReturn(IService.class).when(bindingParameters).getInterfaceClass();
		doReturn(Service.class).when(bindingParameters).getImplementationClass();

		cdi.setManagedLoader(mock(IManagedLoader.class));
		cdi.configure();

		// when
		IService instance = cdi.getInstance(IService.class);

		// then
		assertThat(instance, notNullValue());
		verify(instanceListener, times(1)).onInstanceCreated(instance);
	}

	@Test
	public void GivenTypeREMOTE_WhenGetInstance_ThenNotNull() {
		// given
		doReturn(URI.create("http://localhost/")).when(bindingParameters).getImplementationURL();
		cdi.configure();

		// when
		Object instance = cdi.getInstance(Object.class);

		// then
		assertThat(instance, notNullValue());
	}

	@Test
	public void GivenTypeREMOTE_WhenGetInstance_ThenNoInstanceCreated() {
		// given
		doReturn(URI.create("http://localhost/")).when(bindingParameters).getImplementationURL();
		cdi.configure();

		// when
		cdi.getInstance(Object.class);

		// then
		verify(instanceListener, times(0)).onInstanceCreated(any());
	}

	@Test
	public void GivenTypeSERVICE_WhenGetInstance_ThenNotNull() {
		// given
		doReturn(IService.class).when(bindingParameters).getInterfaceClass();
		doReturn(Service.class).when(bindingParameters).getImplementationClass();
		doReturn(true).when(bindingParameters).isService();
		cdi.configure();

		// when
		IService instance = cdi.getInstance(IService.class);

		// then
		assertThat(instance, notNullValue());
		assertThat(instance.name(), equalTo("service"));
	}

	@Test
	public void GivenTypeSERVICE_WhenGetInstance_ThenNoInstanceCreated() {
		// given
		doReturn(IService.class).when(bindingParameters).getInterfaceClass();
		doReturn(Service.class).when(bindingParameters).getImplementationClass();
		doReturn(true).when(bindingParameters).isService();
		cdi.configure();

		// when
		cdi.getInstance(IService.class);

		// then
		verify(instanceListener, times(0)).onInstanceCreated(any());
	}

	@Test
	public void GivenNoScope_WhenGetInstanceTwice_ThenNotEqual() {
		// given
		cdi.configure();

		// when
		Object instance1 = cdi.getInstance(Object.class);
		Object instance2 = cdi.getInstance(Object.class);

		// then
		assertThat(instance1, notNullValue());
		assertThat(instance2, notNullValue());
		assertThat(instance1, not(equalTo(instance2)));
	}

	@Test
	public void GivenScopeSingleton_WhenGetInstanceTwice_ThenEqual() {
		// given
		doReturn(Singleton.class).when(bindingParameters).getScope();
		cdi.configure();

		// when
		Object instance1 = cdi.getInstance(Object.class);
		Object instance2 = cdi.getInstance(Object.class);

		// then
		assertThat(instance1, notNullValue());
		assertThat(instance2, notNullValue());
		assertThat(instance1, equalTo(instance2));
	}

	@Test
	public void GivenScopeSingleton_WhenGetInstanceFromDifferentThreads_ThenEqual() throws InterruptedException {
		// given
		doReturn(Singleton.class).when(bindingParameters).getScope();
		cdi.configure();

		// when
		Object instance = cdi.getInstance(Object.class);

		final ThreadData<Object> threadData = new ThreadData<>();
		Thread thread = new Thread(() -> {
			threadData.instance = cdi.getInstance(Object.class);
		});
		thread.start();
		thread.join();

		// then
		assertThat(instance, notNullValue());
		assertThat(threadData.instance, notNullValue());
		assertThat(instance, equalTo(threadData.instance));
	}

	@Test
	public void GivenScopeThread_WhenGetInstanceTwice_ThenEqual() {
		// given
		doReturn(ThreadScoped.class).when(bindingParameters).getScope();
		cdi.configure();

		// when
		Object instance1 = cdi.getInstance(Object.class);
		Object instance2 = cdi.getInstance(Object.class);

		// then
		assertThat(instance1, notNullValue());
		assertThat(instance2, notNullValue());
		assertThat(instance1, equalTo(instance2));
	}

	@Test
	public void GivenScopeThread_WhenGetInstanceFromDifferentThreads_ThenNotEqual() throws InterruptedException {
		// given
		doReturn(ThreadScoped.class).when(bindingParameters).getScope();
		cdi.configure();

		// when
		Object instance = cdi.getInstance(Object.class);

		final ThreadData<Object> threadData = new ThreadData<>();
		Thread thread = new Thread(() -> {
			threadData.instance = cdi.getInstance(Object.class);
		});
		thread.start();
		thread.join();

		// then
		assertThat(instance, notNullValue());
		assertThat(threadData.instance, notNullValue());
		assertThat(instance, not(equalTo(threadData.instance)));
	}

	@Test(expected = IllegalStateException.class)
	public void GivenScopeSession_WhenGetInstance_ThenException() {
		// given
		doReturn(SessionScoped.class).when(bindingParameters).getScope();
		cdi.configure();

		// when
		cdi.getInstance(Object.class);

		// then
	}

	@Test
	public void GivenNoScope_WhenGetInstanceTwice_ThenListenerTwice() {
		// given
		cdi.configure();

		// when
		Object instance1 = cdi.getInstance(Object.class);
		Object instance2 = cdi.getInstance(Object.class);

		// then
		verify(instanceListener, times(2)).onInstanceCreated(any());
		verify(instanceListener, times(1)).onInstanceCreated(instance1);
		verify(instanceListener, times(1)).onInstanceCreated(instance2);
	}

	@Test
	public void GivenScopeSingleton_WhenGetInstanceTwice_ThenListenerOnce() {
		// given
		doReturn(Singleton.class).when(bindingParameters).getScope();
		cdi.configure();

		// when
		cdi.getInstance(Object.class);
		cdi.getInstance(Object.class);

		// then
		verify(instanceListener, times(1)).onInstanceCreated(any());
	}

	@Test(expected = IllegalStateException.class)
	public void GivenConfigured_WhenBindInstance_ThenException() {
		// given
		cdi.configure();

		// when
		cdi.bindInstance(Object.class, new Object());

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenConfigured_WhenBindScope_ThenException() {
		// given
		cdi.configure();

		// when
		cdi.bindScope(Singleton.class, mock(IScopeFactory.class));

		// then
	}

	@Test(expected = ProvisionException.class)
	public void GivenNotConfiguredBinding_WhenGetInstance_ThenException() {
		// given

		// when
		cdi.getInstance(Object.class);

		// then
	}

	@Test
	public void GivenScopeSigleton_WhenGetScopeInstance_ThenNull() {
		// given
		doReturn(Singleton.class).when(bindingParameters).getScope();
		cdi.configure();

		// when
		Object instance = cdi.getScopeInstance(Singleton.class, Object.class);

		// then
		assertThat(instance, nullValue());
	}

	@Test
	public void GivenScopeSingletonAndCache_WhenGetScopeInstanceSecondTime_ThenNotNull() {
		// given
		doReturn(Singleton.class).when(bindingParameters).getScope();
		cdi.configure();
		// get instance to fill scope provider cache
		cdi.getInstance(Object.class);

		// when
		Object instance = cdi.getScopeInstance(Singleton.class, Object.class);

		// then
		assertThat(instance, notNullValue());
	}

	@Test
	public void GivenScopeSingletonAndNoCache_WhenGetScopeInstanceSecondTime_ThenNull() {
		// given
		doReturn(Singleton.class).when(bindingParameters).getScope();
		cdi.configure();

		// when
		Object instance = cdi.getScopeInstance(Singleton.class, Object.class);

		// then
		assertThat(instance, nullValue());
	}

	@Test
	public void GivenBindInstance_WhenGetInstance_ThenRetrieveIt() {
		// given
		Object instance = new Object();
		doReturn(instance).when(bindingParameters).getInstance();
		cdi.bindInstance(Object.class, instance);
		cdi.configure();

		// when
		Object instance1 = cdi.getInstance(Object.class);
		Object instance2 = cdi.getInstance(Object.class);

		// then
		assertThat(instance1, equalTo(instance));
		assertThat(instance2, equalTo(instance));
	}

	@Test
	public void GivenBindSessionScope_WhenGetInstance_Then() {
		// given
		cdi.bindScope(SessionScoped.class, new IScopeFactory<Object>() {
			@Override
			public Provider<Object> getScopedProvider(IInjector injector, IBinding<Object> provisioningBinding) {
				return provisioningBinding.provider();
			}
		});

		doReturn(SessionScoped.class).when(bindingParameters).getScope();
		cdi.configure();

		// when
		Object instance = cdi.getInstance(Object.class);

		// then
		assertThat(instance, notNullValue());
	}
}
