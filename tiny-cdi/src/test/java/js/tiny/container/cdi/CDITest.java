package js.tiny.container.cdi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.injector.IScope;
import com.jslib.injector.Key;

import js.tiny.container.fixture.IService;
import js.tiny.container.fixture.Service;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.InstanceScope;
import js.tiny.container.spi.InstanceType;

@RunWith(MockitoJUnitRunner.class)
public class CDITest {
	@Mock
	private IManagedClass<Object> managedClass;
	@Mock
	private IInstancePostConstructionListener<Object> instanceListener;

	private CDI cdi;

	@Before
	public void beforeTest() {
		IScope.clearCache();

		when(managedClass.getInterfaceClass()).thenReturn(Object.class);
		doReturn(Object.class).when(managedClass).getImplementationClass();
		when(managedClass.getInstanceType()).thenReturn(InstanceType.POJO);
		when(managedClass.getInstanceScope()).thenReturn(InstanceScope.LOCAL);

		cdi = CDI.create();
	}

	@Test
	public void GivenTypePOJO_WhenGetInstance_ThenCreateNew() {
		// given
		when(managedClass.getInstanceType()).thenReturn(InstanceType.POJO);
		cdi.configure(Arrays.asList(managedClass));

		// when
		Object instance = cdi.getInstance(Object.class, instanceListener);

		// then
		assertThat(instance, notNullValue());
		verify(instanceListener, times(1)).onInstancePostConstruction(managedClass, instance);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void GivenTypePROXY_WhenGetInstance_ThenCreateNew() {
		// given
		IManagedClass<IService> managedClass = mock(IManagedClass.class);
		IInstancePostConstructionListener<IService> instanceListener = mock(IInstancePostConstructionListener.class);

		when(managedClass.getInterfaceClass()).thenReturn(IService.class);
		doReturn(Service.class).when(managedClass).getImplementationClass();
		when(managedClass.getInstanceType()).thenReturn(InstanceType.PROXY);
		when(managedClass.getInstanceScope()).thenReturn(InstanceScope.LOCAL);

		cdi.configure(Arrays.asList(managedClass));

		// when
		IService instance = cdi.getInstance(IService.class, instanceListener);

		// then
		assertThat(instance, notNullValue());
		verify(instanceListener, times(1)).onInstancePostConstruction(managedClass, instance);
	}

	@Test
	public void GivenTypeREMOTE_WhenGetInstance_ThenCreateNew() {
		// given
		when(managedClass.getInstanceType()).thenReturn(InstanceType.REMOTE);
		when(managedClass.getImplementationURL()).thenReturn("http://localhost/");
		cdi.configure(Arrays.asList(managedClass));

		// when
		Object instance = cdi.getInstance(Object.class, instanceListener);

		// then
		assertThat(instance, notNullValue());
		verify(instanceListener, times(0)).onInstancePostConstruction(any(), any());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void GivenTypeSERVICE_WhenGetInstance_ThenCreateNew() {
		// given
		IManagedClass<IService> managedClass = mock(IManagedClass.class);
		IInstancePostConstructionListener<IService> instanceListener = mock(IInstancePostConstructionListener.class);

		when(managedClass.getInterfaceClass()).thenReturn(IService.class);
		when(managedClass.getInstanceType()).thenReturn(InstanceType.SERVICE);
		when(managedClass.getInstanceScope()).thenReturn(InstanceScope.LOCAL);

		cdi.configure(Arrays.asList(managedClass));

		// when
		IService instance = cdi.getInstance(IService.class, instanceListener);

		// then
		assertThat(instance, notNullValue());
		verify(instanceListener, times(1)).onInstancePostConstruction(managedClass, instance);
		assertThat(instance.name(), equalTo("service"));
	}

	@Test
	public void GivenScopeLOCAL_WhenGetInstanceTwice_ThenNotEqual() {
		// given
		when(managedClass.getInstanceScope()).thenReturn(InstanceScope.LOCAL);
		cdi.configure(Arrays.asList(managedClass));

		// when
		Object instance1 = cdi.getInstance(Object.class, instanceListener);
		Object instance2 = cdi.getInstance(Object.class, instanceListener);

		// then
		assertThat(instance1, notNullValue());
		assertThat(instance2, notNullValue());
		assertThat(instance1, not(equalTo(instance2)));
	}

	@Test
	public void GivenScopeAPPLICATION_WhenGetInstanceTwice_ThenEqual() {
		// given
		when(managedClass.getInstanceScope()).thenReturn(InstanceScope.APPLICATION);
		cdi.configure(Arrays.asList(managedClass));

		// when
		Object instance1 = cdi.getInstance(Object.class, instanceListener);
		Object instance2 = cdi.getInstance(Object.class, instanceListener);

		// then
		assertThat(instance1, notNullValue());
		assertThat(instance2, notNullValue());
		assertThat(instance1, equalTo(instance2));
	}

	@Test
	public void GivenScopeTHREAD_WhenGetInstanceTwice_ThenEqual() {
		// given
		when(managedClass.getInstanceScope()).thenReturn(InstanceScope.THREAD);
		cdi.configure(Arrays.asList(managedClass));

		// when
		Object instance1 = cdi.getInstance(Object.class, instanceListener);
		Object instance2 = cdi.getInstance(Object.class, instanceListener);

		// then
		assertThat(instance1, notNullValue());
		assertThat(instance2, notNullValue());
		assertThat(instance1, equalTo(instance2));
	}

	@Test(expected = IllegalStateException.class)
	public void GivenScopeSESSION_WhenGetInstance_ThenException() {
		// given
		when(managedClass.getInstanceScope()).thenReturn(InstanceScope.SESSION);
		cdi.configure(Arrays.asList(managedClass));

		// when
		cdi.getInstance(Object.class, instanceListener);

		// then
	}

	@Test
	public void GivenScopeLOCAL_WhenGetInstanceTwice_ThenListenerTwice() {
		// given
		when(managedClass.getInstanceScope()).thenReturn(InstanceScope.LOCAL);
		cdi.configure(Arrays.asList(managedClass));

		// when
		Object instance1 = cdi.getInstance(Object.class, instanceListener);
		Object instance2 = cdi.getInstance(Object.class, instanceListener);

		// then
		verify(instanceListener, times(2)).onInstancePostConstruction(eq(managedClass), any());
		verify(instanceListener, times(1)).onInstancePostConstruction(managedClass, instance1);
		verify(instanceListener, times(1)).onInstancePostConstruction(managedClass, instance2);
	}

	@Test
	public void GivenScopeAPPLICATION_WhenGetInstanceTwice_ThenListenerOnce() {
		// given
		when(managedClass.getInstanceScope()).thenReturn(InstanceScope.APPLICATION);
		cdi.configure(Arrays.asList(managedClass));

		// when
		cdi.getInstance(Object.class, instanceListener);
		cdi.getInstance(Object.class, instanceListener);

		// then
		verify(instanceListener, times(1)).onInstancePostConstruction(eq(managedClass), any());
	}

	@Test(expected = IllegalStateException.class)
	public void GivenConfigured_WhenBindInstance_ThenException() {
		// given
		cdi.configure(Arrays.asList(managedClass));

		// when
		cdi.bindInstance(Object.class, new Object());

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenConfigured_WhenBindScope_ThenException() {
		// given
		cdi.configure(Arrays.asList(managedClass));

		// when
		cdi.bindScope(Singleton.class, mock(IScope.class));

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenNotConfigured_WhenGetInstance_ThenException() {
		// given

		// when
		cdi.getInstance(Object.class, instanceListener);

		// then
	}

	@Test
	public void GivenScopeAPPLICATION_WhenGetScopeInstance_ThenNull() {
		// given
		when(managedClass.getInstanceScope()).thenReturn(InstanceScope.APPLICATION);
		cdi.configure(Arrays.asList(managedClass));

		// when
		Object instance = cdi.getScopeInstance(Object.class);

		// then
		assertThat(instance, nullValue());
	}

	@Test
	public void GivenScopeAPPLICATIONAndCache_WhenGetScopeInstanceSecondTime_ThenNotNull() {
		// given
		when(managedClass.getInstanceScope()).thenReturn(InstanceScope.APPLICATION);
		cdi.configure(Arrays.asList(managedClass));
		// get instance to fill scope provider cache
		cdi.getInstance(Object.class, instanceListener);
		
		// when
		Object instance = cdi.getScopeInstance(Object.class);

		// then
		assertThat(instance, notNullValue());
	}

	@Test
	public void GivenScopeAPPLICATIONAndNoCache_WhenGetScopeInstanceSecondTime_ThenNull() {
		// given
		when(managedClass.getInstanceScope()).thenReturn(InstanceScope.APPLICATION);
		cdi.configure(Arrays.asList(managedClass));
		
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
		cdi.configure(Collections.emptyList());

		// when
		Object instance1 = cdi.getInstance(Object.class, instanceListener);
		Object instance2 = cdi.getInstance(Object.class, instanceListener);
		
		
		// then
		assertThat(instance1, equalTo(instance));
		assertThat(instance2, equalTo(instance));
	}

	@Test
	public void GivenBindScope_WhenGetInstance_Then() {
		// given
		cdi.bindScope(SessionScoped.class, new IScope() {
			@Override
			public <T> Provider<T> scope(Key<T> key, Provider<T> provisioningProvider) {
				return provisioningProvider;
			}
		});

		when(managedClass.getInstanceScope()).thenReturn(InstanceScope.SESSION);
		cdi.configure(Arrays.asList(managedClass));

		// when
		Object instance = cdi.getInstance(Object.class, instanceListener);

		// then
		assertThat(instance, notNullValue());
	}

	@Test
	public void Given_When_Then() {
		// given

		// when

		// then
	}
}
