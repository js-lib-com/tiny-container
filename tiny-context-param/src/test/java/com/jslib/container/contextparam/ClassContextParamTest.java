package com.jslib.container.contextparam;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.container.spi.IContainer;
import com.jslib.container.spi.IManagedClass;
import com.jslib.container.spi.ServiceConfigurationException;
import com.jslib.container.spi.IClassPostLoadedProcessor.Priority;

@RunWith(MockitoJUnitRunner.class)
public class ClassContextParamTest {
	@Mock
	private IContainer container;
	@Mock
	private IManagedClass<?> managedClass;

	private ClassContextParam processor;

	@Before
	public void beforeTest() {
		when(container.getInitParameter("field", String.class)).thenReturn("value");

		processor = new ClassContextParam();
		processor.create(container);
	}

	@Test
	public void GivenDefaults_WhenGetPriority_ThenCONSTRCUTOR() {
		// given

		// when
		Priority priority = processor.getPriority();

		// then
		assertThat(priority, equalTo(Priority.INJECT));
	}

	@Test
	public void GivenDefaults_WhenPostLoadClass_ThenFieldIitialized() {
		// given
		doReturn(FieldClass.class).when(managedClass).getImplementationClass();

		// when
		processor.onClassPostLoaded(managedClass);

		// then
		assertThat(FieldClass.field, equalTo("value"));
	}

	@Test(expected = ServiceConfigurationException.class)
	public void GivenFinalField_WhenPostLoadClass_ThenException() {
		// given
		doReturn(FinalFieldClass.class).when(managedClass).getImplementationClass();

		// when
		processor.onClassPostLoaded(managedClass);

		// then
	}

	@Test
	public void GivenMissingOptionalField_WhenPostLoadClass_ThenNullField() {
		// given
		doReturn(FieldClass.class).when(managedClass).getImplementationClass();
		when(container.getInitParameter("field", String.class)).thenReturn(null);

		// when
		processor.onClassPostLoaded(managedClass);

		// then
		assertThat(FieldClass.field, nullValue());
	}

	@Test(expected = NoContextParamException.class)
	public void GivenMissingMandatoryField_WhenPostLoadClass_ThenException() {
		// given
		doReturn(MandatoryFieldClass.class).when(managedClass).getImplementationClass();
		when(container.getInitParameter("field", String.class)).thenReturn(null);

		// when
		processor.onClassPostLoaded(managedClass);

		// then
	}

	// --------------------------------------------------------------------------------------------

	private static class FieldClass {
		@ContextParam(name = "field")
		static String field;
	}

	private static class MandatoryFieldClass {
		@ContextParam(name = "field", mandatory = true)
		static String field;
	}

	private static class FinalFieldClass {
		@ContextParam(name = "field")
		static final String field = "final";
	}
}
