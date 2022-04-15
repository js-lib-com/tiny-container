package js.tiny.container.cdi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
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
	private IBindingBuilder<Object> bindingBuilder;

	private ConfigModule module;

	@Before
	public void beforeTest() {
		when(config.getAttribute("package")).thenReturn("java.lang");
		module = new ConfigModule(config);
	}

	@Test
	public void GivenSingleton_WhenBindInAttribute_ThenInvokedOnce() {
		// given

		// when
		module.bindAttribute(bindingBuilder, "in", "jakarta.inject.Singleton");

		// then
		verify(bindingBuilder, times(1)).in(Singleton.class);
	}

	@Test
	public void GivenShorthandSingleton_WhenBindInAttribute_ThenInvokedOnce() {
		// given

		// when
		module.bindAttribute(bindingBuilder, "in", "Singleton");

		// then
		verify(bindingBuilder, times(1)).in(Singleton.class);
	}

	@Test
	public void GivenSessionScoped_WhenBindInAttribute_ThenInvokedOnce() {
		// given

		// when
		module.bindAttribute(bindingBuilder, "in", "jakarta.enterprise.context.SessionScoped");

		// then
		verify(bindingBuilder, times(1)).in(SessionScoped.class);
	}

	@Test
	public void GivenShorthandSessionScoped_WhenBindInAttribute_ThenInvokedOnce() {
		// given

		// when
		module.bindAttribute(bindingBuilder, "in", "SessionScoped");

		// then
		verify(bindingBuilder, times(1)).in(SessionScoped.class);
	}

	@Test
	public void GivenRequestScoped_WhenBindInAttribute_ThenInvokedOnce() {
		// given

		// when
		module.bindAttribute(bindingBuilder, "in", "jakarta.enterprise.context.RequestScoped");

		// then
		verify(bindingBuilder, times(1)).in(RequestScoped.class);
	}

	@Test
	public void GivenShorthandRequestScoped_WhenBindInAttribute_ThenInvokedOnce() {
		// given

		// when
		module.bindAttribute(bindingBuilder, "in", "RequestScoped");

		// then
		verify(bindingBuilder, times(1)).in(RequestScoped.class);
	}

	@Test(expected = IllegalStateException.class)
	public void GivenMisspelledScope_WhenBindInAttribute_ThenException() {
		// given

		// when
		module.bindAttribute(bindingBuilder, "in", "RequestScopedx");

		// then
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
		verify(bindingBuilder, times(1)).to(Object.class);
	}

	@Test
	public void GivenSimpleName_WhenBindToAttribute_ThenResolveClassName() {
		// given

		// when
		module.bindAttribute(bindingBuilder, "to", "Object");

		// then
		verify(bindingBuilder, times(1)).to(Object.class);
	}

	@Test(expected = IllegalStateException.class)
	public void GivenSimpleNameAndMissingDefaultPackage_WhenBindToAttribute_ThenException() {
		// given
		when(config.getAttribute("package")).thenReturn(null);
		module = new ConfigModule(config);

		// when
		module.bindAttribute(bindingBuilder, "to", "Object");

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenSimpleNameAndBadDefaultPackage_WhenBindToAttribute_ThenException() {
		// given
		when(config.getAttribute("package")).thenReturn("javax.lang"); // misspelled java package
		module = new ConfigModule(config);

		// when
		module.bindAttribute(bindingBuilder, "to", "Object");

		// then
	}

	@Test
	public void GivenAttributeWith_WhenBindAttribute_ThenInvokedOnce() {
		// given

		// when
		module.bindAttribute(bindingBuilder, "with", "jakarta.inject.Named");

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
	public void GivenValidBindClass_WhenConfigure_Then() throws ConfigException {
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
	public void GivenShorthandBindClass_WhenConfigure_Then() throws ConfigException {
		// given
		String descriptor = "<binding bind='Object' />";
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
	public void GivenSubpackageBindClass_WhenConfigure_Then() throws ConfigException {
		// given
		when(config.getAttribute("package")).thenReturn("java");
		module = new ConfigModule(config);
		
		String descriptor = "<binding bind='lang.Object' />";
		ConfigBuilder builder = new ConfigBuilder(descriptor);

		IBindingBuilder<?> bindingBuilder = mock(IBindingBuilder.class);
		IInjector injector = mock(IInjector.class);
		doReturn(bindingBuilder).when(injector).getBindingBuilder(any());
		module.configure(injector);

		// when
		module.configure(builder.build());

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenMisspelledBindClass_WhenConfigure_ThenException() throws ConfigException {
		// given
		String descriptor = "<binding bind='java.lang.Objectx' />";
		ConfigBuilder builder = new ConfigBuilder(descriptor);

		// when
		module.configure(builder.build());

		// then
	}

	@Test(expected = IllegalStateException.class)
	public void GivenMissingBindAttribute_WhenConfigure_ThenException() throws ConfigException {
		// given
		String descriptor = "<binding to='java.lang.Object' />";
		ConfigBuilder builder = new ConfigBuilder(descriptor);

		// when
		module.configure(builder.build());

		// then
	}
}
