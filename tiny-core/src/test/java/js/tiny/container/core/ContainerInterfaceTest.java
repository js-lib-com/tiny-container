package js.tiny.container.core;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.tiny.container.cdi.CDI;
import js.tiny.container.spi.IContainer;

@RunWith(MockitoJUnitRunner.class)
public class ContainerInterfaceTest {
	@Mock
	private CDI cdi;

	private IContainer container;

	@Before
	public void beforeTest() {
		container = new Container(cdi);
	}

	@Test
	public void Given_WhenGetManagedClass_Then() {
		// given

		// when
		container.getManagedClass(Object.class);
		// then
	}
}
