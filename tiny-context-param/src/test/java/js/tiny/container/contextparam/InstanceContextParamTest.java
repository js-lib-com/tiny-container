package js.tiny.container.contextparam;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.tiny.container.servlet.RequestContext;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IInstancePostConstructProcessor.Priority;

@RunWith(MockitoJUnitRunner.class)
public class InstanceContextParamTest {
	@Mock
	private IContainer container;
	@Mock
	private RequestContext requestContext;

	private InstanceContextParam processor;

	@Before
	public void beforeTest() {
		when(container.getInstance(RequestContext.class)).thenReturn(requestContext);
		when(requestContext.getInitParameter(String.class, "field")).thenReturn("value");

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

	@Test(expected = IllegalStateException.class)
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
		when(requestContext.getInitParameter(String.class, "field")).thenReturn(null);

		// when
		processor.onInstancePostConstruct(instance);

		// then
		assertThat(instance.field, nullValue());
	}

	@Test(expected = RuntimeException.class)
	public void GivenMissingMandatoryField_WhenPostConstructInstance_ThenException() {
		// given
		when(requestContext.getInitParameter(String.class, "field")).thenReturn(null);
		MandatoryFieldClass instance = new MandatoryFieldClass();

		// when
		processor.onInstancePostConstruct(instance);

		// then
	}

	// --------------------------------------------------------------------------------------------

	private static class FieldClass {
		@ContextParam("field")
		String field;
	}

	private static class MandatoryFieldClass {
		@ContextParam(value = "field", mandatory = true)
		String field;
	}

	private static class FinalFieldClass {
		@ContextParam("field")
		final String field = "final";
	}
}
