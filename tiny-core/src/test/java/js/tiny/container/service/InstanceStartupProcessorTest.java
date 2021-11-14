package js.tiny.container.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.tiny.container.spi.IContainer;
import js.tiny.container.start.InstanceStartupProcessor;

@RunWith(MockitoJUnitRunner.class)
public class InstanceStartupProcessorTest {
	@Mock
	private IContainer container;

	private InstanceStartupProcessor processor;

	@Before
	public void beforeTest() {
		processor = new InstanceStartupProcessor();
	}

	@Test
	public void Given_When_Then() {
		// given

		// when
		processor.onContainerStart(container);

		// then
	}
}
