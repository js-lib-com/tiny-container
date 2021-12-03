package js.tiny.container.core;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.ConfigException;

@RunWith(MockitoJUnitRunner.class)
public class BootstrapTest {
	@Mock
	private Container container;

	private Bootstrap bootstrap;

	@Before
	public void beforeTest() {
		bootstrap = new Bootstrap();
	}

	@Test
	public void Given_WhenCreateAppContainer_Then() {
		// given

		// when
		bootstrap.createAppContainer();

		// then
	}

	@Test
	public void Given_WhenStartContainer_Then() throws ConfigException {
		// given

		// when
		bootstrap.startContainer(container);

		// then
	}
}
