package com.jslib.container.cdi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
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

import com.jslib.container.spi.IManagedClass;
import com.jslib.container.spi.IManagedMethod;
import com.jslib.lang.BugError;

import jakarta.inject.Provider;

@RunWith(MockitoJUnitRunner.class)
public class ProxyProviderTest {
	@Mock
	private IManagedClass<IPerson> managedClass;
	@Mock
	private IManagedMethod managedMethod;

	@Mock
	private IManagedLoader managedFactory;
	@Mock
	private Provider<IPerson> provider;

	private ProxyProvider<IPerson> proxy;

	@Before
	public void beforeTest() throws Exception {
		when(managedFactory.getManagedClass(IPerson.class)).thenReturn(managedClass);

		when(managedClass.getManagedMethod("name")).thenReturn(managedMethod);
		when(provider.get()).thenReturn(new Person());

		proxy = new ProxyProvider<IPerson>(IPerson.class, managedFactory, provider);
	}

	@Test
	public void GivenDefaults_WhenGet_ThenNotNull() throws Exception {
		// given

		// when
		IPerson instance = proxy.get();

		// then
		assertThat(instance, notNullValue());
	}

	@Test
	public void GivenInstance_WhenInvoke_ThenDelegateManagedMethod() throws Exception {
		// given
		IPerson instance = proxy.get();

		// when
		instance.name();

		// then
		verify(managedMethod, times(1)).invoke(any(), eq((Object[]) null));
	}

	@Test(expected = BugError.class)
	public void GivenMissingManagedMethod_WhenInvoke_ThenException() {
		// given
		when(managedClass.getManagedMethod("name")).thenReturn(null);

		// when
		proxy.get().name();

		// then
	}

	@Test(expected = RuntimeException.class)
	public void GivenExecutionFail_WhenInvoke_ThenException() throws Exception {
		// given
		when(managedMethod.invoke(any(), eq((Object[]) null))).thenThrow(RuntimeException.class);

		// when
		proxy.get().name();

		// then
	}

	@Test
	public void GivenProvider_WhenToString_ThenContainsPROXY() {
		// given

		// when
		String string = proxy.toString();

		// then
		assertThat(string, containsString("PROXY"));
	}

	// --------------------------------------------------------------------------------------------

	private interface IPerson {
		String name();
	}

	private static class Person implements IPerson {
		@Override
		public String name() {
			return "John Doe";
		}
	}
}
