package js.tiny.container.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import jakarta.annotation.PostConstruct;
import js.injector.IModule;
import js.lang.Config;
import js.lang.ConfigException;
import js.tiny.container.cdi.CDI;
import js.tiny.container.cdi.IClassBinding;
import js.tiny.container.spi.IManagedClass;

@RunWith(MockitoJUnitRunner.class)
public class ContainerLifeCycleTest {
	@Mock
	private CDI cdi;
	@Mock
	private IClassBinding<?> binding;
	@Mock
	private Config config;

	private Container container;

	@Before
	public void beforeTest() {
		when(cdi.configure(config)).thenReturn(Arrays.asList(binding));
		container = new Container(cdi);
	}

	@Test
	public void GivenDescriptor_WhenConfig_ThenClassesPoolContainsManagedClass() throws ConfigException {
		// given
		doReturn(Service.class).when(binding).getInterfaceClass();
		doReturn(Service.class).when(binding).getImplementationClass();

		// when
		container.configure(config);

		// then
		assertThat(container.getManagedClasses().get(0), notNullValue());
	}

	@Test
	public void GivenMissingInterfaceAttribute_WhenConfig_ThenAddIt() throws ConfigException {
		// given
		doReturn(Service.class).when(binding).getInterfaceClass();
		doReturn(Service.class).when(binding).getImplementationClass();

		// when
		container.configure(config);

		// then
		IManagedClass<?> managedClass = container.getManagedClasses().get(0);
		assertThat(managedClass, notNullValue());
		assertThat(managedClass.getInterfaceClass(), equalTo(Service.class));
		assertThat(managedClass.getImplementationClass(), equalTo(Service.class));
	}

	@Test
	public void GivenMissingManagedClassesSection_WhenConfig_ThenEmptyClassesPool() throws ConfigException {
		// given
		when(cdi.configure(config)).thenReturn(Collections.emptyList());

		// when
		container.configure(config);

		// then
		assertThat(container.getManagedClasses().size(), equalTo(0));
	}

	@Test
	public void GivenDefaults_WhenConfig_ThenCDIConfigure() throws ConfigException {
		// given
		doReturn(Service.class).when(binding).getInterfaceClass();
		doReturn(Service.class).when(binding).getImplementationClass();

		// when
		container.configure(config);

		// then
		verify(cdi, times(1)).configure(any(Config.class));
	}

	@Test
	public void GivenModule_WhenConfig_ThenCDIConfigure() throws ConfigException {
		// given
		IModule module = mock(IModule.class);

		// when
		container.modules(module);

		// then
		verify(cdi, times(1)).configure(module);
	}

	@Test
	public void Given_WhenStart_Then() {
		// given

		// when
		container.start();

		// then
	}

	@Test
	public void GivenManagedClass_WhenClose_Then() {
		// given
		doReturn(Service.class).when(binding).getInterfaceClass();
		doReturn(Service.class).when(binding).getImplementationClass();
		container.create(Arrays.asList(binding));

		// when
		container.close();

		// then
	}

	@Test
	public void GivenNoManagedClass_WhenClose_Then() {
		// given
		doReturn(Service.class).when(binding).getInterfaceClass();
		doReturn(Service.class).when(binding).getImplementationClass();
		container.create(Arrays.asList(binding));

		// when
		container.close();

		// then
	}

	@Test
	public void GivenCloseException_WhenClose_ThenExceptionSwallowed() {
		// given

		// when
		container.close();

		// then
	}

	@Test
	public void GivenManagedClass_WhenOnInstanceCreated_Then() {
		// given
		Object instance = new Object();
		container.managedImplementations().put(Object.class, mock(ManagedClass.class));

		// when
		container.onInstanceCreated(instance);

		// then
	}

	@Test
	public void GivenNoManagedClass_WhenOnInstanceCreated_Then() {
		// given
		Object instance = new Object();

		// when
		container.onInstanceCreated(instance);

		// then
	}

	// --------------------------------------------------------------------------------------------

	private static class Service {
		@PostConstruct
		private void postConstruct() {
		}
	}
}
