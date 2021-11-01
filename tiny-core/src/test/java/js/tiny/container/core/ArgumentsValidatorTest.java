package js.tiny.container.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.VarArgs;
import js.tiny.container.spi.IManagedMethod;

@RunWith(MockitoJUnitRunner.class)
public class ArgumentsValidatorTest {
	@Mock
	private IManagedMethod managedMethod;

	private ArgumentsValidator validator;

	@Before
	public void beforeTest() {
		validator = new ArgumentsValidator();
	}

	@Test
	public void GivenEmptyParameters_WhenValidateArguments_ThenEmptyArguments() {
		// given
		when(managedMethod.getParameterTypes()).thenReturn(new Type[] {});
		Object[] arguments = new Object[] {};

		// when
		arguments = validator.validateArguments(managedMethod, arguments);

		// then
		assertThat(arguments, notNullValue());
		assertThat(arguments.length, equalTo(0));
	}

	@Test
	public void GivenNullArguments_WhenValidateArguments_ThenEmptyArguments() {
		// given

		// when
		Object[] arguments = validator.validateArguments(managedMethod, null);

		// then
		assertThat(arguments, notNullValue());
		assertThat(arguments.length, equalTo(0));
	}

	@Test
	public void GivenArgumentsMatch_WhenValidateArguments_ThenReturnArguments() {
		// given
		when(managedMethod.getParameterTypes()).thenReturn(new Type[] { String.class, int.class });
		Object[] arguments = new Object[] { "John Doe", 50 };

		// when
		arguments = validator.validateArguments(managedMethod, arguments);

		// then
		assertThat(arguments, notNullValue());
		assertThat(arguments.length, equalTo(2));
		assertThat(arguments[0], equalTo("John Doe"));
		assertThat(arguments[1], equalTo(50));
	}

	@Test
	public void GivenVarArg_WhenValidateArguments_ThenReturnArguments() {
		// given
		when(managedMethod.getParameterTypes()).thenReturn(new Type[] { Object[].class });
		Object[] arguments = new Object[] { new VarArgs<Object>("John Doe", 50) };

		// when
		arguments = validator.validateArguments(managedMethod, arguments);

		// then
		assertThat(arguments, notNullValue());
		assertThat(arguments.length, equalTo(1));
		assertThat(arguments[0], equalTo(new Object[] { "John Doe", 50 }));
	}

	@Test(expected = IllegalArgumentException.class)
	public void GivenArgumentCountMiss_WhenValidateArguments_ThenException() {
		// given
		when(managedMethod.getParameterTypes()).thenReturn(new Type[] {});
		Object[] arguments = new Object[] { 50 };

		// when
		validator.validateArguments(managedMethod, arguments);

		// then
	}

	@Test(expected = IllegalArgumentException.class)
	public void GivenArgumentTypeMiss_WhenValidateArguments_ThenException() {
		// given
		when(managedMethod.getParameterTypes()).thenReturn(new Type[] { String.class });
		Object[] arguments = new Object[] { 50 };

		// when
		validator.validateArguments(managedMethod, arguments);

		// then
	}
}
