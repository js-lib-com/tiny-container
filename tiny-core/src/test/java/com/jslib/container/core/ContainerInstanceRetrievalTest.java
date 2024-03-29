package com.jslib.container.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.api.injector.ProvisionException;
import com.jslib.container.cdi.CDI;
import com.jslib.container.spi.IManagedClass;

import jakarta.inject.Singleton;

@RunWith(MockitoJUnitRunner.class)
public class ContainerInstanceRetrievalTest {
	@Mock
	private CDI cdi;
	@Mock
	private IManagedClass<Object> managedClass;

	private Container container;

	@Before
	public void beforeTest() {
		when(cdi.getInstance(eq(Object.class))).thenReturn(new Object());

		container = new Container();
		container.init(cdi);
	}

	/** Container has managed class registered and CDI has related injector binding. */
	@Test
	public void GivenExistingManagedClass_WhenGetInstance_ThenNotNull() {
		// given
		container.managedInterfaces().put(Object.class, managedClass);

		// when
		Object instance = container.getInstance(Object.class);

		// then
		assertThat(instance, notNullValue());
		verify(cdi, times(1)).getInstance(any());
	}

	/** Container has managed class registered but CDI has not related injector binding. */
	@Test(expected = ProvisionException.class)
	public void GivenCDIException_WhenGetInstance_ThenException() {
		// given
		container.managedInterfaces().put(Object.class, managedClass);
		when(cdi.getInstance(any())).thenThrow(ProvisionException.class);

		// when
		container.getInstance(Object.class);

		// then
	}

	/** Requested managed class is not registered to container. */
	@Test(expected = ProvisionException.class)
	public void GivenMissingManagedClass_WhenGetInstance_ThenException() {
		// given
		when(cdi.getInstance(eq(Object.class))).thenThrow(ProvisionException.class);

		// when
		container.getInstance(Object.class);

		// then
		verify(cdi, times(0)).getInstance(any());
	}

	/** Interface class argument is null. */
	@Test(expected = IllegalArgumentException.class)
	public void GivenNullInterfaceClass_WhenGetInstance_ThenException() {
		// given

		// when
		container.getInstance((Class<Object>) null);

		// then
		verify(cdi, times(0)).getInstance(any());
	}

	@Test
	public void GivenExistingManagedClass_WhenGetOptionalInstance_ThenNotNull() {
		// given
		container.managedInterfaces().put(Object.class, managedClass);

		// when
		Object instance = container.getOptionalInstance(Object.class);

		// then
		assertThat(instance, notNullValue());
		verify(cdi, times(1)).getInstance(any());
	}

	@Test
	public void GivenCDIException_WhenGetOptionalInstance_ThenNull() {
		// given
		container.managedInterfaces().put(Object.class, managedClass);
		when(cdi.getInstance(any())).thenThrow(ProvisionException.class);

		// when
		Object instance = container.getOptionalInstance(Object.class);

		// then
		assertThat(instance, nullValue());
		verify(cdi, times(1)).getInstance(any());
	}

	@Test
	public void GivenMissingManagedClass_WhenGetOptionalInstance_ThenNull() {
		// given
		when(cdi.getInstance(eq(Object.class))).thenThrow(ProvisionException.class);

		// when
		Object instance = container.getOptionalInstance(Object.class);

		// then
		assertThat(instance, nullValue());
		verify(cdi, times(1)).getInstance(any());
	}

	@Test(expected = IllegalArgumentException.class)
	public void GivenNullInterfaceClass_WhenGetOptionalInstance_ThenException() {
		// given

		// when
		container.getOptionalInstance((Class<Object>) null);

		// then
		verify(cdi, times(0)).getInstance(any());
	}

	@Test
	public void GivenExistingScopeBindings_WhenGetScopeInstance_ThenNotNull() {
		// given
		when(cdi.getScopeInstance(Singleton.class, Object.class)).thenReturn(Object.class);
		
		// when
		Object instance = container.getScopeInstance(Singleton.class, Object.class);

		// then
		assertThat(instance, notNullValue());
		verify(cdi, times(1)).getScopeInstance(eq(Singleton.class), any());
	}

	@Test
	public void GivenMissingManagedClass_WhenGetScopeInstance_ThenNull() {
		// given

		// when
		Object instance = container.getScopeInstance(Singleton.class, Object.class);

		// then
		assertThat(instance, nullValue());
		verify(cdi, times(1)).getScopeInstance(any(), any());
	}
}
