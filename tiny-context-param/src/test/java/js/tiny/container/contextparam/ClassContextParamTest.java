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

import js.tiny.container.contextparam.ClassContextParam.Provider;
import js.tiny.container.servlet.RequestContext;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IManagedClass;

@RunWith(MockitoJUnitRunner.class)
public class ClassContextParamTest {
	@Mock
	private IContainer container;
	@Mock
	private IManagedClass<?> managedClass;
	@Mock
	private RequestContext requestContext;

	private ClassContextParam processor;

	@Before
	public void beforeTest() {
		BusinessClass.staticField = null;

		when(container.getInstance(RequestContext.class)).thenReturn(requestContext);
		when(requestContext.getInitParameter(any(), any())).thenReturn("value");
		doReturn(BusinessClass.class).when(managedClass).getImplementationClass();

		ClassContextParam.Provider provider = new Provider();
		processor = provider.getService(container);
	}

	@Test
	public void GivenDefaults_WhenPostLoadClass_ThenFieldIitialized() {
		// given

		// when
		processor.onClassPostLoaded(managedClass);

		// then
		assertThat(BusinessClass.staticField, equalTo("value"));
	}

	@Test
	public void GivenMissingOptionalField_WhenPostLoadClass_ThenNullField() {
		// given
		when(requestContext.getInitParameter(String.class, "static.field")).thenReturn(null);

		// when
		processor.onClassPostLoaded(managedClass);

		// then
		assertThat(BusinessClass.staticField, nullValue());
	}

	@Test(expected = RuntimeException.class)
	public void GivenMissingMandatoryField_WhenPostLoadClass_ThenException() {
		// given
		when(requestContext.getInitParameter(String.class, "static.mandatory.field")).thenReturn(null);

		// when
		processor.onClassPostLoaded(managedClass);

		// then
	}
}
