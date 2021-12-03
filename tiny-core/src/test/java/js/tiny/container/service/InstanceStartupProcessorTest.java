package js.tiny.container.service;

import static org.mockito.Mockito.*;

import java.util.Arrays;

import javax.ejb.Startup;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IManagedClass;

@RunWith(MockitoJUnitRunner.class)
public class InstanceStartupProcessorTest {
	@Mock
	private IContainer container;
	@Mock
	private IManagedClass<?> managedClass;

	private InstanceStartupProcessor processor;

	@Before
	public void beforeTest() {
		when(container.getManagedClasses()).thenReturn(Arrays.asList(managedClass));
		
		processor = new InstanceStartupProcessor();
	}

	@Test
	public void GivenStartupAnnotation_WhenOnContainerStart_Then() {
		// given
		when(managedClass.scanAnnotation(Startup.class)).thenReturn(mock(Startup.class));
		
		// when
		processor.onContainerStart(container);

		// then
	}

	@Test
	public void GivenNoStartupAnnotation_WhenOnContainerStart_Then() {
		// given

		// when
		processor.onContainerStart(container);

		// then
	}
}
