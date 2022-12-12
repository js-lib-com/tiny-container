package com.jslib.container.cdi;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.api.injector.ProvisionException;
import com.jslib.lang.Config;

import jakarta.inject.Inject;

@RunWith(MockitoJUnitRunner.class)
public class CircularDependencyTest {
	@Mock
	private Config config;

	private CDI cdi;

	@Before
	public void beforeTest() {
		cdi = CDI.create();
	}

	@Test
	public void GivenFieldCircularDependency_WhenGetInstance_ThenException() {
		// given
		cdi.bind(new BindingParameters<>(FieldService.class));
		cdi.configure(config);

		// when
		String exception = null;
		try {
			cdi.getInstance(FieldService.class);
		} catch (ProvisionException e) {
			exception = e.getMessage();
		}

		// then
		assertThat(exception, notNullValue());
		assertThat(exception, containsString("Circular dependency"));
	}

	@Test
	public void GivenConstructorCircularDependency_WhenGetInstance_ThenException() {
		// given
		cdi.bind(new BindingParameters<>(ConstructorService.class));
		cdi.configure(config);

		// when
		String exception = null;
		try {
			cdi.getInstance(ConstructorService.class);
		} catch (IllegalStateException e) {
			exception = e.getMessage();
		}

		// then
		assertThat(exception, notNullValue());
		assertThat(exception, startsWith("Circular dependency"));
	}

	@Test
	public void GivenMethodCircularDependency_WhenGetInstance_ThenException() {
		// given
		cdi.bind(new BindingParameters<>(MethodService.class));
		cdi.configure(config);

		// when
		String exception = null;
		try {
			cdi.getInstance(MethodService.class);
		} catch (ProvisionException e) {
			exception = e.getMessage();
		}

		// then
		assertThat(exception, notNullValue());
		assertThat(exception, containsString("Circular dependency"));
	}

	@Test
	public void GivenGrandfatherCircularDependency_WhenGetInstance_ThenException() {
		// given
		cdi.bind(new BindingParameters<>(Grandfather.class));
		cdi.bind(new BindingParameters<>(Father.class));
		cdi.bind(new BindingParameters<>(Son.class));
		cdi.configure(config);

		// when
		String exception = null;
		try {
			cdi.getInstance(Grandfather.class);
		} catch (ProvisionException e) {
			exception = e.getMessage();
		}

		// then
		assertThat(exception, notNullValue());
		assertThat(exception, containsString("Circular dependency"));
	}

	// --------------------------------------------------------------------------------------------

	private static class FieldService {
		@SuppressWarnings("unused")
		@Inject
		private FieldService service;
	}

	private static class ConstructorService {
		@Inject
		public ConstructorService(ConstructorService service) {
		}
	}

	private static class MethodService {
		@Inject
		public void setService(MethodService service) {
		}
	}

	private static class Grandfather {
		@SuppressWarnings("unused")
		@Inject
		private Father father;
	}

	private static class Father {
		@SuppressWarnings("unused")
		@Inject
		private Son son;
	}

	private static class Son {
		@SuppressWarnings("unused")
		@Inject
		private Grandfather grandfather;
	}
}
