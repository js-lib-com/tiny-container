package com.jslib.container.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.container.spi.IManagedClass;
import com.jslib.container.spi.IManagedMethod;
import com.jslib.container.spi.IInstancePreDestroyProcessor.Priority;

import jakarta.annotation.PreDestroy;

@RunWith(MockitoJUnitRunner.class)
public class InstancePreDestructorTest {
	@Mock
	private IManagedClass<Service> managedClass;
	@Mock
	private IManagedMethod managedMethod;
	@Mock
	private jakarta.annotation.PreDestroy jakartaPreDestroy;

	private Object instance;

	private InstancePreDestructor processor;

	@Before
	public void beforeTest() {
		instance = new Service();
		doReturn(Service.class).when(managedClass).getImplementationClass();
		when(managedClass.getManagedMethods()).thenReturn(Arrays.asList(managedMethod));
		when(managedMethod.getParameterTypes()).thenReturn(new Type[0]);
		when(managedMethod.getExceptionTypes()).thenReturn(new Type[0]);
		when(managedMethod.getReturnType()).thenReturn(Void.class);

		processor = new InstancePreDestructor();
	}

	@Test
	public void GivenSuccessfulBind_WhenInstancePreDestroy_Then() {
		// given
		when(managedMethod.scanAnnotation(jakarta.annotation.PreDestroy.class)).thenReturn(jakartaPreDestroy);
		processor.bind(managedClass);

		// when
		processor.onInstancePreDestroy(instance);

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenNoBind_WhenInstancePreDestroy_ThenException() {
		// given

		// when
		processor.onInstancePreDestroy(instance);

		// then
	}

	@Test
	public void GivenExceptionalService_WhenInstancePreDestroy_Then() {
		// given
		when(managedMethod.scanAnnotation(jakarta.annotation.PreDestroy.class)).thenReturn(jakartaPreDestroy);
		instance = new ExceptionalService();
		doReturn(ExceptionalService.class).when(managedClass).getImplementationClass();
		processor.bind(managedClass);

		// when
		processor.onInstancePreDestroy(instance);

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenStaticMethod_WhenInstancePreDestroy_Then() {
		// given
		instance = new StaticService();
		processor.bind(managedClass);

		// when
		processor.onInstancePreDestroy(instance);

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenParameterMethod_WhenInstancePreDestroy_Then() {
		// given
		instance = new ParameterService();
		processor.bind(managedClass);

		// when
		processor.onInstancePreDestroy(instance);

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenNonVoidMethod_WhenInstancePreDestroy_Then() {
		// given
		instance = new NonVoidService();
		processor.bind(managedClass);

		// when
		processor.onInstancePreDestroy(instance);

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenThrowsMethod_WhenInstancePreDestroy_Then() {
		// given
		instance = new ThrowsService();
		processor.bind(managedClass);

		// when
		processor.onInstancePreDestroy(instance);

		// then
	}

	@Test
	public void GivenProcessor_WhenGetPriority_ThenDESTRUCTOR() {
		// given

		// when
		Priority priority = processor.getPriority();

		// then
		assertThat(priority, equalTo(Priority.DESTRUCTOR));
	}

	// --------------------------------------------------------------------------------------------

	private static class Service {
		@PreDestroy
		private void postConstruct() {
		}
	}

	private static class ExceptionalService {
		@PreDestroy
		private void postConstruct() {
			throw new RuntimeException();
		}
	}

	private static class StaticService {
		@PreDestroy
		private static void postConstruct() {
		}
	}

	private static class ParameterService {
		@PreDestroy
		private void postConstruct(String name) {
		}
	}

	private static class NonVoidService {
		@PreDestroy
		private String postConstruct() {
			return "name";
		}
	}

	private static class ThrowsService {
		@PreDestroy
		private void postConstruct() throws Exception {
		}
	}
}
