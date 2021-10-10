package js.tiny.container.contextparam;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.tiny.container.servlet.RequestContext;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IManagedClass;

@RunWith(MockitoJUnitRunner.class)
public class ContextParamProcessorTest {
	@Mock
	private IContainer container;
	@Mock
	private IManagedClass managedClass;

	@Mock
	private RequestContext requestContext;

	private ContextParamProcessor processor;

	@Before
	public void beforeTest() {
		BusinessClass.staticField = null;

		when(container.getInstance(RequestContext.class)).thenReturn(requestContext);
		when(requestContext.getInitParameter(any(), any())).thenReturn("value");
		doReturn(BusinessClass.class).when(managedClass).getImplementationClass();

		processor = new ContextParamProcessor(container);
	}

	@Test
	public void Given_WhenPostLoadClass_Then() {
		// given

		// when
		processor.postLoadClass(managedClass);

		// then
		assertThat(BusinessClass.staticField, equalTo("value"));
	}

	@Test
	public void GivenMissingOptionalField_WhenPostLoadClass_ThenNull() {
		// given
		when(requestContext.getInitParameter(String.class, "static.field")).thenReturn(null);

		// when
		processor.postLoadClass(managedClass);

		// then
		assertThat(BusinessClass.staticField, nullValue());
	}

	@Test(expected = RuntimeException.class)
	public void GivenMissingMandatoryField_WhenPostLoadClass_ThenException() {
		// given
		when(requestContext.getInitParameter(String.class, "static.mandatory.field")).thenReturn(null);

		// when
		processor.postLoadClass(managedClass);

		// then
		assertThat(BusinessClass.staticField, nullValue());
	}

	@Test
	public void Given_WhenPostConstructInstance_Then() {
		// given
		BusinessClass instance = new BusinessClass();

		// when
		processor.postConstructInstance(managedClass, instance);

		// then
		assertThat(instance.instanceField, equalTo("value"));
	}

	private static class BusinessClass {
		@ContextParam("static.field")
		private static String staticField;

		@ContextParam(value = "static.mandatory.field", mandatory = true)
		private static String staticMandatoryField;

		@ContextParam("instance.field")
		private String instanceField;
	}
}
