package js.tiny.container.cdi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.injector.IScope;
import com.jslib.injector.Key;

import js.lang.Config;
import js.tiny.container.fixture.IService;
import js.tiny.container.fixture.Service;
import js.tiny.container.fixture.ThreadData;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.InstanceScope;
import js.tiny.container.spi.InstanceType;

@RunWith(MockitoJUnitRunner.class)
public class CDIUnitTest {
	@Mock
	private Config config;
	@Mock
	private Config managedClassConfig;
	@Mock
	private Function<Class<?>, IManagedClass<?>> managedClassFactory;
	@Mock
	private IInstanceCreatedListener instanceListener;

	private CDI cdi;

	@Before
	public void beforeTest() {
		IScope.clearCache();

		when(config.getChildren()).thenReturn(Arrays.asList(managedClassConfig));
		when(managedClassConfig.getAttribute("interface", Class.class, Object.class)).thenReturn(Object.class);
		when(managedClassConfig.getAttribute("class", Class.class)).thenReturn(Object.class);
		when(managedClassConfig.getAttribute("type", InstanceType.class, InstanceType.POJO)).thenReturn(InstanceType.POJO);
		when(managedClassConfig.getAttribute("scope", InstanceScope.class, InstanceScope.APPLICATION)).thenReturn(InstanceScope.LOCAL);

		cdi = CDI.create();
		cdi.setInstanceCreatedListener(instanceListener);
	}

	@Test
	public void GivenTypePOJO_WhenGetInstance_ThenCreateNew() {
		// given
		cdi.configure(config, managedClassFactory);

		// when
		Object instance = cdi.getInstance(Object.class);

		// then
		assertThat(instance, notNullValue());
		verify(instanceListener, times(1)).onInstanceCreated(instance);
	}

	@Test
	public void GivenTypePROXY_WhenGetInstance_ThenCreateNew() {
		// given
		when(managedClassConfig.getAttribute("interface", Class.class, Service.class)).thenReturn(IService.class);
		when(managedClassConfig.getAttribute("class", Class.class)).thenReturn(Service.class);
		when(managedClassConfig.getAttribute("type", InstanceType.class, InstanceType.POJO)).thenReturn(InstanceType.PROXY);
		when(managedClassConfig.getAttribute("scope", InstanceScope.class, InstanceScope.APPLICATION)).thenReturn(InstanceScope.LOCAL);

		cdi.configure(config, managedClassFactory);

		// when
		IService instance = cdi.getInstance(IService.class);

		// then
		assertThat(instance, notNullValue());
		verify(instanceListener, times(1)).onInstanceCreated(instance);
	}

	@Test
	public void GivenTypeREMOTE_WhenGetInstance_ThenNotNull() {
		// given
		when(managedClassConfig.getAttribute("type", InstanceType.class, InstanceType.POJO)).thenReturn(InstanceType.REMOTE);
		when(managedClassConfig.getAttribute("url", URI.class)).thenReturn(URI.create("http://localhost/"));
		cdi.configure(config, managedClassFactory);

		// when
		Object instance = cdi.getInstance(Object.class);

		// then
		assertThat(instance, notNullValue());
	}

	@Test
	public void GivenTypeREMOTE_WhenGetInstance_ThenNoInstanceCreated() {
		// given
		when(managedClassConfig.getAttribute("type", InstanceType.class, InstanceType.POJO)).thenReturn(InstanceType.REMOTE);
		when(managedClassConfig.getAttribute("url", URI.class)).thenReturn(URI.create("http://localhost/"));
		cdi.configure(config, managedClassFactory);

		// when
		cdi.getInstance(Object.class);

		// then
		verify(instanceListener, times(0)).onInstanceCreated(any());
	}

	@Test
	public void GivenTypeSERVICE_WhenGetInstance_ThenNotNull() {
		// given
		when(managedClassConfig.getAttribute("interface", Class.class, Service.class)).thenReturn(IService.class);
		when(managedClassConfig.getAttribute("class", Class.class)).thenReturn(Service.class);
		when(managedClassConfig.getAttribute("type", InstanceType.class, InstanceType.POJO)).thenReturn(InstanceType.SERVICE);
		cdi.configure(config, managedClassFactory);

		// when
		IService instance = cdi.getInstance(IService.class);

		// then
		assertThat(instance, notNullValue());
		assertThat(instance.name(), equalTo("service"));
	}

	@Test
	public void GivenTypeSERVICE_WhenGetInstance_ThenNoInstanceCreated() {
		// given
		when(managedClassConfig.getAttribute("interface", Class.class, Service.class)).thenReturn(IService.class);
		when(managedClassConfig.getAttribute("class", Class.class)).thenReturn(Service.class);
		when(managedClassConfig.getAttribute("type", InstanceType.class, InstanceType.POJO)).thenReturn(InstanceType.SERVICE);
		cdi.configure(config, managedClassFactory);

		// when
		cdi.getInstance(IService.class);

		// then
		verify(instanceListener, times(0)).onInstanceCreated(any());
	}

	@Test
	public void GivenScopeLOCAL_WhenGetInstanceTwice_ThenNotEqual() {
		// given
		when(managedClassConfig.getAttribute("scope", InstanceScope.class, InstanceScope.APPLICATION)).thenReturn(InstanceScope.LOCAL);
		cdi.configure(config, managedClassFactory);

		// when
		Object instance1 = cdi.getInstance(Object.class);
		Object instance2 = cdi.getInstance(Object.class);

		// then
		assertThat(instance1, notNullValue());
		assertThat(instance2, notNullValue());
		assertThat(instance1, not(equalTo(instance2)));
	}

	@Test
	public void GivenScopeAPPLICATION_WhenGetInstanceTwice_ThenEqual() {
		// given
		when(managedClassConfig.getAttribute("scope", InstanceScope.class, InstanceScope.APPLICATION)).thenReturn(InstanceScope.APPLICATION);
		cdi.configure(config, managedClassFactory);

		// when
		Object instance1 = cdi.getInstance(Object.class);
		Object instance2 = cdi.getInstance(Object.class);

		// then
		assertThat(instance1, notNullValue());
		assertThat(instance2, notNullValue());
		assertThat(instance1, equalTo(instance2));
	}

	@Test
	public void GivenScopeAPPLICATION_WhenGetInstanceFromDifferentThreads_ThenEqual() throws InterruptedException {
		// given
		when(managedClassConfig.getAttribute("scope", InstanceScope.class, InstanceScope.APPLICATION)).thenReturn(InstanceScope.APPLICATION);
		cdi.configure(config, managedClassFactory);

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
	public void GivenScopeTHREAD_WhenGetInstanceTwice_ThenEqual() {
		// given
		when(managedClassConfig.getAttribute("scope", InstanceScope.class, InstanceScope.APPLICATION)).thenReturn(InstanceScope.THREAD);
		cdi.configure(config, managedClassFactory);

		// when
		Object instance1 = cdi.getInstance(Object.class);
		Object instance2 = cdi.getInstance(Object.class);

		// then
		assertThat(instance1, notNullValue());
		assertThat(instance2, notNullValue());
		assertThat(instance1, equalTo(instance2));
	}

	@Test
	public void GivenScopeTHREAD_WhenGetInstanceFromDifferentThreads_ThenNotEqual() throws InterruptedException {
		// given
		when(managedClassConfig.getAttribute("scope", InstanceScope.class, InstanceScope.APPLICATION)).thenReturn(InstanceScope.THREAD);
		cdi.configure(config, managedClassFactory);

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
	public void GivenScopeSESSION_WhenGetInstance_ThenException() {
		// given
		when(managedClassConfig.getAttribute("scope", InstanceScope.class, InstanceScope.APPLICATION)).thenReturn(InstanceScope.SESSION);
		cdi.configure(config, managedClassFactory);

		// when
		cdi.getInstance(Object.class);

		// then
	}

	@Test
	public void GivenScopeLOCAL_WhenGetInstanceTwice_ThenListenerTwice() {
		// given
		when(managedClassConfig.getAttribute("scope", InstanceScope.class, InstanceScope.APPLICATION)).thenReturn(InstanceScope.LOCAL);
		cdi.configure(config, managedClassFactory);

		// when
		Object instance1 = cdi.getInstance(Object.class);
		Object instance2 = cdi.getInstance(Object.class);

		// then
		verify(instanceListener, times(2)).onInstanceCreated(any());
		verify(instanceListener, times(1)).onInstanceCreated(instance1);
		verify(instanceListener, times(1)).onInstanceCreated(instance2);
	}

	@Test
	public void GivenScopeAPPLICATION_WhenGetInstanceTwice_ThenListenerOnce() {
		// given
		when(managedClassConfig.getAttribute("scope", InstanceScope.class, InstanceScope.APPLICATION)).thenReturn(InstanceScope.APPLICATION);
		cdi.configure(config, managedClassFactory);

		// when
		cdi.getInstance(Object.class);
		cdi.getInstance(Object.class);

		// then
		verify(instanceListener, times(1)).onInstanceCreated(any());
	}

	@Test(expected = IllegalStateException.class)
	public void GivenConfigured_WhenBindInstance_ThenException() {
		// given
		cdi.configure(config, managedClassFactory);

		// when
		cdi.bindInstance(Object.class, new Object());

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenConfigured_WhenBindScope_ThenException() {
		// given
		cdi.configure(config, managedClassFactory);

		// when
		cdi.bindScope(Singleton.class, mock(IScope.class));

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenNotConfigured_WhenGetInstance_ThenException() {
		// given

		// when
		cdi.getInstance(Object.class);

		// then
	}

	@Test
	public void GivenScopeAPPLICATION_WhenGetScopeInstance_ThenNull() {
		// given
		when(managedClassConfig.getAttribute("scope", InstanceScope.class, InstanceScope.APPLICATION)).thenReturn(InstanceScope.APPLICATION);
		cdi.configure(config, managedClassFactory);

		// when
		Object instance = cdi.getScopeInstance(Object.class);

		// then
		assertThat(instance, nullValue());
	}

	@Test
	public void GivenScopeAPPLICATIONAndCache_WhenGetScopeInstanceSecondTime_ThenNotNull() {
		// given
		when(managedClassConfig.getAttribute("scope", InstanceScope.class, InstanceScope.APPLICATION)).thenReturn(InstanceScope.APPLICATION);
		cdi.configure(config, managedClassFactory);
		// get instance to fill scope provider cache
		cdi.getInstance(Object.class);

		// when
		Object instance = cdi.getScopeInstance(Object.class);

		// then
		assertThat(instance, notNullValue());
	}

	@Test
	public void GivenScopeAPPLICATIONAndNoCache_WhenGetScopeInstanceSecondTime_ThenNull() {
		// given
		when(managedClassConfig.getAttribute("scope", InstanceScope.class, InstanceScope.APPLICATION)).thenReturn(InstanceScope.APPLICATION);
		cdi.configure(config, managedClassFactory);

		// when
		Object instance = cdi.getScopeInstance(Object.class);

		// then
		assertThat(instance, nullValue());
	}

	@Test
	public void GivenBindInstance_WhenGetInstance_ThenRetrieveIt() {
		// given
		Object instance = new Object();
		cdi.bindInstance(Object.class, instance);

		when(config.getChildren()).thenReturn(Collections.emptyList());
		cdi.configure(config, managedClassFactory);

		// when
		Object instance1 = cdi.getInstance(Object.class);
		Object instance2 = cdi.getInstance(Object.class);

		// then
		assertThat(instance1, equalTo(instance));
		assertThat(instance2, equalTo(instance));
	}

	@Test
	public void GivenBindScope_WhenGetInstance_Then() {
		// given
		cdi.bindScope(SessionScoped.class, new IScope<Object>() {
			@Override
			public Provider<Object> scope(Key<Object> key, Provider<Object> provisioningProvider) {
				return provisioningProvider;
			}
		});

		when(managedClassConfig.getAttribute("scope", InstanceScope.class, InstanceScope.APPLICATION)).thenReturn(InstanceScope.SESSION);
		cdi.configure(config, managedClassFactory);

		// when
		Object instance = cdi.getInstance(Object.class);

		// then
		assertThat(instance, notNullValue());
	}
}
