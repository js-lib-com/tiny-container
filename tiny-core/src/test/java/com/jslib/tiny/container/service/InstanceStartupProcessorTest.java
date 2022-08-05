package com.jslib.tiny.container.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.tiny.container.spi.IContainer;
import com.jslib.tiny.container.spi.IManagedClass;

import jakarta.ejb.Startup;

@RunWith(MockitoJUnitRunner.class)
public class InstanceStartupProcessorTest {
	@Mock
	private IContainer container;
	@Mock
	private IManagedClass<?> managedClass;

	private ManagedInstanceStartupProcessor processor;

	@Before
	public void beforeTest() {
		when(container.getManagedClasses()).thenReturn(Arrays.asList(managedClass));
		
		processor = new ManagedInstanceStartupProcessor();
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
