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
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.injector.IScope;
import com.jslib.injector.Key;

import js.tiny.container.fixture.IService;
import js.tiny.container.fixture.Service;
import js.tiny.container.fixture.ThreadData;
import js.tiny.container.spi.IClassDescriptor;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.InstanceScope;
import js.tiny.container.spi.InstanceType;

@RunWith(MockitoJUnitRunner.class)
public class CDITest {
	@Mock
	private IClassDescriptor<Object> classDescriptor;
	@Mock
	private Function<IClassDescriptor<?>, IManagedClass<?>> managedClassFactory;
	@Mock
	private IInstancePostConstructionListener<Object> instanceListener;

	private CDI cdi;

	@Before
	public void beforeTest() {
		IScope.clearCache();

		when(classDescriptor.getInterfaceClass()).thenReturn(Object.class);
		doReturn(Object.class).when(classDescriptor).getImplementationClass();
		when(classDescriptor.getInstanceType()).thenReturn(InstanceType.POJO);
		when(classDescriptor.getInstanceScope()).thenReturn(InstanceScope.LOCAL);

		cdi = CDI.create();
	}

	@Test
	public void GivenTypePOJO_WhenGetInstance_ThenCreateNew() {
		// given
		when(classDescriptor.getInstanceType()).thenReturn(InstanceType.POJO);
		cdi.configure(Arrays.asList(classDescriptor), managedClassFactory);

		// when
		Object instance = cdi.getInstance(Object.class, instanceListener);

		// then
		assertThat(instance, notNullValue());
		verify(instanceListener, times(1)).onInstancePostConstruction(instance);
	}

	@Test
	@SuppressWarnings("unchecked")
	@Ignore
	public void GivenTypePROXY_WhenGetInstance_ThenCreateNew() {
		// given
		IClassDescriptor<IService> managedClass = mock(IClassDescriptor.class);
		IInstancePostConstructionListener<IService> instanceListener = mock(IInstancePostConstructionListener.class);

		when(managedClass.getInterfaceClass()).thenReturn(IService.class);
		doReturn(Service.class).when(managedClass).getImplementationClass();
		when(managedClass.getInstanceType()).thenReturn(InstanceType.PROXY);
		when(managedClass.getInstanceScope()).thenReturn(InstanceScope.LOCAL);

		cdi.configure(Arrays.asList(classDescriptor), managedClassFactory);

		// when
		IService instance = cdi.getInstance(IService.class, instanceListener);

		// then
		assertThat(instance, notNullValue());
		verify(instanceListener, times(1)).onInstancePostConstruction(instance);
	}

	@Test
	public void GivenTypeREMOTE_WhenGetInstance_ThenCreateNew() {
		// given
		when(classDescriptor.getInstanceType()).thenReturn(InstanceType.REMOTE);
		when(classDescriptor.getImplementationURL()).thenReturn("http://localhost/");
		cdi.configure(Arrays.asList(classDescriptor), managedClassFactory);

		// when
		Object instance = cdi.getInstance(Object.class, instanceListener);

		// then
		assertThat(instance, notNullValue());
		verify(instanceListener, times(0)).onInstancePostConstruction(any());
	}

	@Test
	@SuppressWarnings("unchecked")
	@Ignore
	public void GivenTypeSERVICE_WhenGetInstance_ThenCreateNew() {
		// given
		IClassDescriptor<IService> managedClass = mock(IClassDescriptor.class);
		IInstancePostConstructionListener<IService> instanceListener = mock(IInstancePostConstructionListener.class);

		when(managedClass.getInterfaceClass()).thenReturn(IService.class);
		when(managedClass.getInstanceType()).thenReturn(InstanceType.SERVICE);
		when(managedClass.getInstanceScope()).thenReturn(InstanceScope.LOCAL);

		cdi.configure(Arrays.asList(classDescriptor), managedClassFactory);

		// when
		IService instance = cdi.getInstance(IService.class, instanceListener);

		// then
		assertThat(instance, notNullValue());
		verify(instanceListener, times(1)).onInstancePostConstruction(instance);
		assertThat(instance.name(), equalTo("service"));
	}

	@Test
	public void GivenScopeLOCAL_WhenGetInstanceTwice_ThenNotEqual() {
		// given
		when(classDescriptor.getInstanceScope()).thenReturn(InstanceScope.LOCAL);
		cdi.configure(Arrays.asList(classDescriptor), managedClassFactory);

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
		when(classDescriptor.getInstanceScope()).thenReturn(InstanceScope.APPLICATION);
		cdi.configure(Arrays.asList(classDescriptor), managedClassFactory);

		// when
		Object instance1 = cdi.getInstance(Object.class, instanceListener);
		Object instance2 = cdi.getInstance(Object.class, instanceListener);

		// then
		assertThat(instance1, notNullValue());
		assertThat(instance2, notNullValue());
		assertThat(instance1, equalTo(instance2));
	}

	@Test
	public void GivenScopeAPPLICATION_WhenGetInstanceFromDifferentThreads_ThenEqual() throws InterruptedException {
		// given
		when(classDescriptor.getInstanceScope()).thenReturn(InstanceScope.APPLICATION);
		cdi.configure(Arrays.asList(classDescriptor), managedClassFactory);

		// when
		Object instance = cdi.getInstance(Object.class, instanceListener);
		
		final ThreadData<Object> threadData = new ThreadData<>();
		Thread thread = new Thread(() -> {
			threadData.instance = cdi.getInstance(Object.class, instanceListener);
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
		when(classDescriptor.getInstanceScope()).thenReturn(InstanceScope.THREAD);
		cdi.configure(Arrays.asList(classDescriptor), managedClassFactory);

		// when
		Object instance1 = cdi.getInstance(Object.class, instanceListener);
		Object instance2 = cdi.getInstance(Object.class, instanceListener);

		// then
		assertThat(instance1, notNullValue());
		assertThat(instance2, notNullValue());
		assertThat(instance1, equalTo(instance2));
	}

	@Test
	public void GivenScopeTHREAD_WhenGetInstanceFromDifferentThreads_ThenNotEqual() throws InterruptedException {
		// given
		when(classDescriptor.getInstanceScope()).thenReturn(InstanceScope.THREAD);
		cdi.configure(Arrays.asList(classDescriptor), managedClassFactory);

		// when
		Object instance = cdi.getInstance(Object.class, instanceListener);

		final ThreadData<Object> threadData = new ThreadData<>();
		Thread thread = new Thread(() -> {
			threadData.instance = cdi.getInstance(Object.class, instanceListener);
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
		when(classDescriptor.getInstanceScope()).thenReturn(InstanceScope.SESSION);
		cdi.configure(Arrays.asList(classDescriptor), managedClassFactory);

		// when
		cdi.getInstance(Object.class, instanceListener);

		// then
	}

	@Test
	public void GivenScopeLOCAL_WhenGetInstanceTwice_ThenListenerTwice() {
		// given
		when(classDescriptor.getInstanceScope()).thenReturn(InstanceScope.LOCAL);
		cdi.configure(Arrays.asList(classDescriptor), managedClassFactory);

		// when
		Object instance1 = cdi.getInstance(Object.class, instanceListener);
		Object instance2 = cdi.getInstance(Object.class, instanceListener);

		// then
		verify(instanceListener, times(2)).onInstancePostConstruction(any());
		verify(instanceListener, times(1)).onInstancePostConstruction(instance1);
		verify(instanceListener, times(1)).onInstancePostConstruction(instance2);
	}

	@Test
	public void GivenScopeAPPLICATION_WhenGetInstanceTwice_ThenListenerOnce() {
		// given
		when(classDescriptor.getInstanceScope()).thenReturn(InstanceScope.APPLICATION);
		cdi.configure(Arrays.asList(classDescriptor), managedClassFactory);

		// when
		cdi.getInstance(Object.class, instanceListener);
		cdi.getInstance(Object.class, instanceListener);

		// then
		verify(instanceListener, times(1)).onInstancePostConstruction(any());
	}

	@Test(expected = IllegalStateException.class)
	public void GivenConfigured_WhenBindInstance_ThenException() {
		// given
		cdi.configure(Arrays.asList(classDescriptor), managedClassFactory);

		// when
		cdi.bindInstance(Object.class, new Object());

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenConfigured_WhenBindScope_ThenException() {
		// given
		cdi.configure(Arrays.asList(classDescriptor), managedClassFactory);

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
		when(classDescriptor.getInstanceScope()).thenReturn(InstanceScope.APPLICATION);
		cdi.configure(Arrays.asList(classDescriptor), managedClassFactory);

		// when
		Object instance = cdi.getScopeInstance(Object.class);

		// then
		assertThat(instance, nullValue());
	}

	@Test
	public void GivenScopeAPPLICATIONAndCache_WhenGetScopeInstanceSecondTime_ThenNotNull() {
		// given
		when(classDescriptor.getInstanceScope()).thenReturn(InstanceScope.APPLICATION);
		cdi.configure(Arrays.asList(classDescriptor), managedClassFactory);
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
		when(classDescriptor.getInstanceScope()).thenReturn(InstanceScope.APPLICATION);
		cdi.configure(Arrays.asList(classDescriptor), managedClassFactory);

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
		cdi.configure(Collections.emptyList(), managedClassFactory);

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
		cdi.bindScope(SessionScoped.class, new IScope<Object>() {
			@Override
			public Provider<Object> scope(Key<Object> key, Provider<Object> provisioningProvider) {
				return provisioningProvider;
			}
		});

		when(classDescriptor.getInstanceScope()).thenReturn(InstanceScope.SESSION);
		cdi.configure(Arrays.asList(classDescriptor), managedClassFactory);

		// when
		Object instance = cdi.getInstance(Object.class, instanceListener);

		// then
		assertThat(instance, notNullValue());
	}
}
