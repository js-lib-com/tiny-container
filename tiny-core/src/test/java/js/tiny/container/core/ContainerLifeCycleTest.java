package js.tiny.container.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.Config;
import js.lang.ConfigException;
import js.tiny.container.cdi.Binding;
import js.tiny.container.cdi.CDI;
import js.tiny.container.spi.IManagedClass;

@RunWith(MockitoJUnitRunner.class)
public class ContainerLifeCycleTest {
	@Mock
	private CDI cdi;
	@Mock
	private Binding<Object> binding;
	@Mock
	private Config config;

	private Container container;

	@Before
	public void beforeTest() {
		when(binding.getInterfaceClass()).thenReturn(Object.class);
		doReturn(Object.class).when(binding).getImplementationClass();
		when(cdi.configure(any(), any())).thenReturn(Arrays.asList(binding));
		
		container = new Container(cdi);
	}

	@Test
	public void GivenDescriptor_WhenConfig_ThenClassesPoolContainsManagedClass() throws ConfigException {
		// given

		// when
		container.config(config);

		// then
		assertThat(container.getManagedClasses().get(0), notNullValue());
	}

	@Test
	public void GivenMissingInterfaceAttribute_WhenConfig_ThenAddIt() throws ConfigException {
		// given
		
		
		// when
		container.config(config);

		// then
		IManagedClass<?> managedClass = container.getManagedClasses().get(0);
		assertThat(managedClass, notNullValue());
		assertThat(managedClass.getInterfaceClass(), equalTo(Object.class));
		assertThat(managedClass.getImplementationClass(), equalTo(Object.class));
	}

	@Test
	public void GivenMissingManagedClassesSection_WhenConfig_ThenEmptyClassesPool() throws ConfigException {
		// given
		when(cdi.configure(any(), any())).thenReturn(Collections.emptyList());

		// when
		container.config(config);

		// then
		assertThat(container.getManagedClasses().size(), equalTo(0));
	}

	@Test
	public void GivenDefaults_WhenConfig_ThenCDIConfigure() throws ConfigException {
		// given

		// when
		container.config(config);

		// then
		verify(cdi, times(1)).configure(any(Config.class), any());
	}

	@Test
	public void Given_WhenStart_Then() {
		// given

		// when
		container.start();

		// then
	}

	@Test
	public void Given_WhenClose_Then() {
		// given

		// when
		container.close();

		// then
	}
}
