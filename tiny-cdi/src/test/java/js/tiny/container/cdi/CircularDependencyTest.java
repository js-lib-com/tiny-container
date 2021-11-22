package js.tiny.container.cdi;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.util.function.Function;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.Config;
import js.tiny.container.spi.IManagedClass;

@RunWith(MockitoJUnitRunner.class)
public class CircularDependencyTest {
	@Mock
	private Config config;
	@Mock
	private Function<Class<?>, IManagedClass<?>> managedClassFactory;

	private CDI cdi;

	@Before
	public void beforeTest() {
		cdi = CDI.create();
	}

	@Test
	public void GivenFieldCircularDependency_WhenGetInstance_ThenException() {
		// given
		cdi.bind(new Binding<>(FieldService.class));
		cdi.configure(config, managedClassFactory);

		// when
		String exception = null;
		try {
			cdi.getInstance(FieldService.class);
		} catch (IllegalStateException e) {
			exception = e.getMessage();
		}

		// then
		assertThat(exception, notNullValue());
		assertThat(exception, startsWith("Circular dependency"));
	}

	@Test
	public void GivenConstructorCircularDependency_WhenGetInstance_ThenException() {
		// given
		cdi.bind(new Binding<>(ConstructorService.class));
		cdi.configure(config, managedClassFactory);

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
		cdi.bind(new Binding<>(MethodService.class));
		cdi.configure(config, managedClassFactory);

		// when
		String exception = null;
		try {
			cdi.getInstance(MethodService.class);
		} catch (IllegalStateException e) {
			exception = e.getMessage();
		}

		// then
		assertThat(exception, notNullValue());
		assertThat(exception, startsWith("Circular dependency"));
	}

	@Test
	public void GivenGrandfatherCircularDependency_WhenGetInstance_ThenException() {
		// given
		cdi.bind(new Binding<>(Grandfather.class));
		cdi.bind(new Binding<>(Father.class));
		cdi.bind(new Binding<>(Son.class));
		cdi.configure(config, managedClassFactory);

		// when
		String exception = null;
		try {
			cdi.getInstance(Grandfather.class);
		} catch (IllegalStateException e) {
			exception = e.getMessage();
		}

		// then
		assertThat(exception, notNullValue());
		assertThat(exception, startsWith("Circular dependency"));
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
