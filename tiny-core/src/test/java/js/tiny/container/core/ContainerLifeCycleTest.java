package js.tiny.container.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.Config;
import js.lang.ConfigBuilder;
import js.lang.ConfigException;
import js.tiny.container.cdi.CDI;
import js.tiny.container.spi.IClassDescriptor;
import js.tiny.container.spi.IManagedClass;

@RunWith(MockitoJUnitRunner.class)
public class ContainerLifeCycleTest {
	@Mock
	private CDI cdi;
	@Mock
	private Config config;
	@Mock
	private IClassDescriptor<Object> descriptor;

	private Container container;

	@Before
	public void beforeTest() {
		when(descriptor.getInterfaceClass()).thenReturn(Object.class);
		doReturn(Object.class).when(descriptor).getImplementationClass();
		
		container = new Container(cdi);
	}

	@Test
	public void GivenDescriptor_WhenConfig_ThenClassesPoolContainsManagedClass() throws ConfigException {
		// given

		// when
		container.config(Arrays.asList(descriptor));

		// then
		assertThat(container.classesPool().get(Object.class), notNullValue());
	}

	@Test
	public void GivenMissingInterfaceAttribute_WhenConfig_ThenAddIt() throws ConfigException {
		// given
		
		
		// when
		container.config(Arrays.asList(descriptor));

		// then
		IManagedClass<?> managedClass = container.classesPool().get(Object.class);
		assertThat(managedClass, notNullValue());
		assertThat(managedClass.getInterfaceClass(), equalTo(Object.class));
		assertThat(managedClass.getImplementationClass(), equalTo(Object.class));
	}

	@Test
	public void GivenMissingManagedClassesSection_WhenConfig_ThenEmptyClassesPool() throws ConfigException {
		// given
		String descriptor = "<config>" + //
				"	<managed-classes-fake>" + //
				"		<test class='java.lang.Object' />" + //
				"	</managed-classes-fake>" + //
				"</config>";
		ConfigBuilder builder = new ConfigBuilder(descriptor);

		// when
		container.config(builder.build());

		// then
		assertThat(container.classesPool(), anEmptyMap());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void GivenDefaults_WhenConfig_ThenCDIConfigure() throws ConfigException {
		// given

		// when
		container.config(Arrays.asList(descriptor));

		// then
		verify(cdi, times(1)).configure(any(List.class), any());
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
