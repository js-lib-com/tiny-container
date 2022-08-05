package com.jslib.container.mvc;

import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.container.mvc.annotation.Controller;
import com.jslib.container.spi.IManagedClass;

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
		when(managedClass.scanAnnotation(Controller.class)).thenReturn(controller);
		
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
