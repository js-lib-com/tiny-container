package js.tiny.container.mvc;

import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.tiny.container.mvc.annotation.Controller;
import js.tiny.container.spi.IManagedClass;

@RunWith(MockitoJUnitRunner.class)
public class ResourceConnectorTest {
	@Mock
	private IManagedClass<?> managedClass;
	@Mock
	private Controller controller;
	
	@Mock
	private MethodsCache cache;

	private ResourceConnector connector;

	@Before
	public void beforeTest() {
		when(managedClass.getAnnotation(Controller.class)).thenReturn(controller);
		
		connector = new ResourceConnector(cache);
	}

	@Test
	public void Given_WhenOnClassPostLoaded_Then() {
		// given

		// when
		connector.onClassPostLoaded(managedClass);
		
		// then
	}

	@Test
	public void Given_When_Then() {
		// given

		// when

		// then
	}
}
