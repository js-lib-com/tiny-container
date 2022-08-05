package com.jslib.container.async;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.container.spi.IContainer;
import com.jslib.container.spi.IInvocation;
import com.jslib.container.spi.IInvocationProcessorsChain;
import com.jslib.container.spi.IManagedMethod;
import com.jslib.container.spi.IThreadsPool;
import com.jslib.container.spi.ServiceConfigurationException;
import com.jslib.container.spi.IMethodInvocationProcessor.Priority;

import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;

@RunWith(MockitoJUnitRunner.class)
public class AsyncServiceTest {
	@Mock
	private IContainer container;
	@Mock
	private IThreadsPool threadsPool;
	@Mock
	private IManagedMethod managedMethod;
	@Mock
	private Asynchronous asynchronous;
	@Mock
	private IInvocationProcessorsChain chain;
	@Mock
	private IInvocation invocation;

	private AsyncService service;

	@Before
	public void beforeTest() throws Exception {
		when(container.getInstance(IThreadsPool.class)).thenReturn(threadsPool);
		when(invocation.method()).thenReturn(managedMethod);

		service = new AsyncService();
		service.create(container);
	}

	@After
	public void afterTest() {
		service.destroy();
	}

	@Test
	public void GivenDefaults_WhenGetPriority_ThenASYNCHRONOUS() {
		// given

		// when
		Priority priority = service.getPriority();

		// then
		assertThat(priority, equalTo(Priority.ASYNCHRONOUS));
	}

	@Test
	public void GivenNotAsynchronous_WhenBind_ThenNotBound() {
		// given

		// when
		boolean bound = service.bind(managedMethod);

		// then
		assertThat(bound, equalTo(false));
	}

	@Test(expected = ServiceConfigurationException.class)
	public void GivenStaticAsynchronous_WhenBind_ThenException() {
		// given
		when(managedMethod.scanAnnotation(jakarta.ejb.Asynchronous.class, IManagedMethod.Flags.INCLUDE_TYPES)).thenReturn(asynchronous);
		when(managedMethod.isStatic()).thenReturn(true);

		// when
		service.bind(managedMethod);

		// then
	}

	@Test
	public void GivenVoidAsynchronous_WhenBind_ThenBound() {
		// given
		when(managedMethod.scanAnnotation(jakarta.ejb.Asynchronous.class, IManagedMethod.Flags.INCLUDE_TYPES)).thenReturn(asynchronous);
		when(managedMethod.isVoid()).thenReturn(true);
		when(managedMethod.getExceptionTypes()).thenReturn(new Type[0]);

		// when
		boolean bound = service.bind(managedMethod);

		// then
		assertThat(bound, equalTo(true));
	}

	@Test(expected = ServiceConfigurationException.class)
	public void GivenVoidAsynchronousWithException_WhenBind_ThenException() {
		// given
		when(managedMethod.scanAnnotation(jakarta.ejb.Asynchronous.class, IManagedMethod.Flags.INCLUDE_TYPES)).thenReturn(asynchronous);
		when(managedMethod.isVoid()).thenReturn(true);
		when(managedMethod.getExceptionTypes()).thenReturn(new Type[] { Exception.class });

		// when
		service.bind(managedMethod);

		// then
	}

	@Test
	public void GivenFutureAsynchronous_WhenBind_ThenBound() {
		// given
		when(managedMethod.scanAnnotation(jakarta.ejb.Asynchronous.class, IManagedMethod.Flags.INCLUDE_TYPES)).thenReturn(asynchronous);
		when(managedMethod.getReturnType()).thenReturn(Future.class);

		// when
		boolean bound = service.bind(managedMethod);

		// then
		assertThat(bound, equalTo(true));
	}

	@Test(expected = ServiceConfigurationException.class)
	public void GivenNonFutureAsynchronous_WhenBind_ThenException() {
		// given
		when(managedMethod.scanAnnotation(jakarta.ejb.Asynchronous.class, IManagedMethod.Flags.INCLUDE_TYPES)).thenReturn(asynchronous);
		when(managedMethod.getReturnType()).thenReturn(Object.class);

		// when
		service.bind(managedMethod);

		// then
	}

	@Test
	public void GivenVoidMethod_WhenOnMethodInvocation_ThenThreadsPoolExecute() throws Exception {
		// given
		when(managedMethod.isVoid()).thenReturn(true);

		// when
		service.onMethodInvocation(chain, invocation);

		// then
		verify(threadsPool, times(1)).execute(any(), any());
		verify(threadsPool, times(0)).submit(any(), any());
	}

	@Test
	public void GivenFutureMethod_WhenOnMethodInvocation_ThenChainNextProcessor() throws Exception {
		// given
		when(threadsPool.submit(any(), any())).thenReturn(new AsyncResult<Object>("Tom Joad"));

		// when
		@SuppressWarnings("unchecked")
		Future<String> future = (Future<String>) service.onMethodInvocation(chain, invocation);

		// then
		assertThat(future.get(), equalTo("Tom Joad"));
	}
}
