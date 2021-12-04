package js.tiny.container.cdi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.*;

import java.lang.annotation.Annotation;

import javax.inject.Named;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.injector.IBindingBuilder;
import js.injector.IInjector;
import js.lang.Config;
import js.lang.ConfigBuilder;
import js.lang.ConfigException;

@RunWith(MockitoJUnitRunner.class)
public class ConfigModuleTest {
	@Mock
	private Config config;
	@Mock
	private IBindingBuilder<?> bindingBuilder;

	private ConfigModule module;

	@Before
	public void beforeTest() {
		module = new ConfigModule(config);
	}

	@Test
	public void GivenAttributeIn_WhenBindAttribute_ThenInvokedOnce() {
		// given

		// when
		module.bindAttribute(bindingBuilder, "in", "javax.inject.Singleton");

		// then
		verify(bindingBuilder, times(1)).in(any());
	}

	@Test
	public void GivenAttributeNamed_WhenBindAttribute_ThenInvokedOnce() {
		// given

		// when
		module.bindAttribute(bindingBuilder, "named", "developer");

		// then
		verify(bindingBuilder, times(1)).named(any());
	}

	@Test
	public void GivenAttributeOn_WhenBindAttribute_ThenInvokedOnce() {
		// given

		// when
		module.bindAttribute(bindingBuilder, "on", "http://localhost/");

		// then
		verify(bindingBuilder, times(1)).on(any(String.class));
	}

	@Test
	public void GivenAttributeService_WhenBindAttribute_ThenInvokedOnce() {
		// given

		// when
		module.bindAttribute(bindingBuilder, "service", "true");

		// then
		verify(bindingBuilder, times(1)).service();
	}

	@Test
	public void GivenAttributeTo_WhenBindAttribute_ThenInvokedOnce() {
		// given

		// when
		module.bindAttribute(bindingBuilder, "to", "java.lang.Object");

		// then
		verify(bindingBuilder, times(1)).to(any());
	}

	@Test
	public void GivenAttributeWith_WhenBindAttribute_ThenInvokedOnce() {
		// given

		// when
		module.bindAttribute(bindingBuilder, "with", "javax.inject.Named");

		// then
		verify(bindingBuilder, times(1)).with(Named.class);
	}

	@Test
	public void GivenAttributeBind_WhenBindAttribute_ThenDoNothing() {
		// given

		// when
		module.bindAttribute(bindingBuilder, "bind", "java.lang.Object");

		// then
		verify(bindingBuilder, times(0)).in(any());
		verify(bindingBuilder, times(0)).named(any());
		verify(bindingBuilder, times(0)).on(any(String.class));
		verify(bindingBuilder, times(0)).service();
		verify(bindingBuilder, times(0)).to(any());
		verify(bindingBuilder, times(0)).with(any(Annotation.class));
	}

	@Test(expected = IllegalStateException.class)
	public void GivenNotExistingAttribute_WhenBindAttribute_ThenException() {
		// given

		// when
		module.bindAttribute(bindingBuilder, "fake", null);

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenAttributeException_WhenBindAttribute_Thenexception() {
		// given
		doThrow(RuntimeException.class).when(bindingBuilder).named(any(String.class));

		// when
		module.bindAttribute(bindingBuilder, "named", "developer");

		// then
	}

	@Test
	public void Given_WhenConstructor_Then() throws ConfigException {
		// given
		String descriptor = "" + //
				"<module>" + //
				"	<binding bind='java.lang.Object' />" + //
				"</module>";
		ConfigBuilder builder = new ConfigBuilder(descriptor);

		// when
		@SuppressWarnings("unused")
		ConfigModule module = new ConfigModule(builder.build());
		
		// then
	}

	@Test
	public void Given_WhenConfigure_Then() throws ConfigException {
		// given
		String descriptor = "<binding bind='java.lang.Object' />";
		ConfigBuilder builder = new ConfigBuilder(descriptor);
		
		IBindingBuilder<?> bindingBuilder = mock(IBindingBuilder.class);
		IInjector injector = mock(IInjector.class);
		doReturn(bindingBuilder).when(injector).getBindingBuilder(any());
		module.configure(injector);

		// when
		module.configure(builder.build());

		// then
	}

	@Test
	public void Given_When_Then() {
		// given

		// when

		// then
	}
}
