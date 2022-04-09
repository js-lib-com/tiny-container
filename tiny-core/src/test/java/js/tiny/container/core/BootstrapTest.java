package js.tiny.container.core;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.Config;
import js.lang.ConfigException;

@RunWith(MockitoJUnitRunner.class)
public class BootstrapTest {
	@Mock
	private Container container;
	@Mock
	private Config config;

	private Bootstrap bootstrap;

	@Before
	public void beforeTest() {
		bootstrap = new Bootstrap();
	}

	@Test
	public void GivenEmptyConfigObject_WhenCreateAppContainer_ThenNoException() {
		// given

		// when
		bootstrap.createAppContainer(config);

		// then
	}

	@Test
	public void GivenEmptyConfigObject_WhenStartContainer_ThenNoException() throws ConfigException {
		// given

		// when
		bootstrap.startContainer(container, config);

		// then
	}
}
