package com.jslib.tiny.container.core;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.api.injector.IScopeFactory;
import com.jslib.tiny.container.cdi.CDI;
import com.jslib.tiny.container.spi.IContainer;

import jakarta.inject.Singleton;

@RunWith(MockitoJUnitRunner.class)
public class ContainerTest {
	@Mock
	private CDI cdi;

	private IContainer container;

	@Before
	public void beforeTest() {
		container = new Container(cdi);
	}

	@Test
	public void Given_WhenGetInstance_Then() {
		// given

		// when
		container.getInstance(Object.class);

		// then
		verify(cdi, times(1)).getInstance(Object.class);
	}

	@Test
	public void Given_WhenGetManagedClass_Then() {
		// given

		// when
		container.getManagedClass(Object.class);

		// then
	}

	@Test
	public void Given_WhenBindScope_Then() {
		// given
		IScopeFactory<?> scope = mock(IScopeFactory.class);

		// when
		container.bindScope(Singleton.class, scope);

		// then
	}
}
