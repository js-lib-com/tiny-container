package com.jslib.container.contextparam;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.container.spi.IContainer;
import com.jslib.container.spi.ServiceConfigurationException;
import com.jslib.container.spi.IInstancePostConstructProcessor.Priority;

@RunWith(MockitoJUnitRunner.class)
public class InstanceContextParamTest {
	@Mock
	private IContainer container;

	private InstanceContextParam processor;

	@Before
	public void beforeTest() {
		when(container.getInitParameter("field", String.class)).thenReturn("value");

		processor = new InstanceContextParam();
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
	public void GivenDefaults_WhenPostConstructInstance_ThenFieldInitialized() {
		// given
		FieldClass instance = new FieldClass();

		// when
		processor.onInstancePostConstruct(instance);

		// then
		assertThat(instance.field, equalTo("value"));
	}

	@Test
	public void GivenCustomParser_WhenPostConstructInstance_ThenFieldInitialized() {
		// given
		CustomParserClass instance = new CustomParserClass();

		// when
		processor.onInstancePostConstruct(instance);

		// then
		assertThat(instance.field, equalTo("custom"));
	}

	@Test
	public void GivenCustomParserFailAndNotMandatory_WhenPostConstructInstance_ThenNullField() {
		// given
		CustomParserFailClass instance = new CustomParserFailClass();

		// when
		processor.onInstancePostConstruct(instance);

		// then
		assertThat(instance.field, nullValue());
	}

	@Test(expected = RuntimeException.class)
	public void MandatoryGivenCustomParserFailAndNotMandatory_WhenPostConstructInstance_ThenException() {
		// given
		MandatoryCustomParserFailClass instance = new MandatoryCustomParserFailClass();

		// when
		processor.onInstancePostConstruct(instance);

		// then
		assertThat(instance.field, nullValue());
	}

	@Test(expected = ServiceConfigurationException.class)
	public void GivenFinalField_WhenPostLoadClass_ThenException() {
		// given
		FinalFieldClass instance = new FinalFieldClass();

		// when
		processor.onInstancePostConstruct(instance);

		// then
	}

	@Test
	public void GivenMissingOptionalField_WhenPostConstructInstance_ThenNullField() {
		// given
		FieldClass instance = new FieldClass();
		when(container.getInitParameter("field", String.class)).thenReturn(null);

		// when
		processor.onInstancePostConstruct(instance);

		// then
		assertThat(instance.field, nullValue());
	}

	@Test(expected = NoContextParamException.class)
	public void GivenMissingMandatoryField_WhenPostConstructInstance_ThenException() {
		// given
		when(container.getInitParameter("field", String.class)).thenReturn(null);
		MandatoryFieldClass instance = new MandatoryFieldClass();

		// when
		processor.onInstancePostConstruct(instance);

		// then
	}

	// --------------------------------------------------------------------------------------------

	private static class FieldClass {
		@ContextParam(name = "field")
		String field;
	}

	private static class MandatoryFieldClass {
		@ContextParam(name = "field", mandatory = true)
		String field;
	}

	private static class FinalFieldClass {
		@ContextParam(name = "field")
		final String field = "final";
	}

	private static class CustomParser implements ContextParam.Parser {
		@Override
		public Object parse(String value) throws Exception {
			return "custom";
		}
	}

	private static class CustomParserClass {
		@ContextParam(name = "field", parser = CustomParser.class)
		String field;
	}

	private static class CustomParserFail implements ContextParam.Parser {
		@Override
		public Object parse(String value) throws Exception {
			throw new Exception("Parse fail.");
		}
	}

	private static class CustomParserFailClass {
		@ContextParam(name = "field", parser = CustomParserFail.class)
		String field;
	}

	private static class MandatoryCustomParserFailClass {
		@ContextParam(name = "field", parser = CustomParserFail.class, mandatory = true)
		String field;
	}
}
