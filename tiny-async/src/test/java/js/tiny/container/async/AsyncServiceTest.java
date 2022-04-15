package js.tiny.container.async;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import js.tiny.container.spi.IInvocation;
import js.tiny.container.spi.IInvocationProcessorsChain;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IMethodInvocationProcessor.Priority;
import js.tiny.container.spi.ServiceConfigurationException;

@RunWith(MockitoJUnitRunner.class)
public class AsyncServiceTest {
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
		when(invocation.method()).thenReturn(managedMethod);

		service = new AsyncService(Executors.newSingleThreadExecutor());
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
	public void GivenVoidMethod_WhenOnMethodInvocation_ThenChainNextProcessor() throws Exception {
		// given
		Object lock = new Object();

		when(managedMethod.isVoid()).thenReturn(true);
		when(chain.invokeNextProcessor(invocation)).thenAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				synchronized (lock) {
					lock.notify();
					return null;
				}
			}
		});

		// when
		service.onMethodInvocation(chain, invocation);

		// then
		synchronized (lock) {
			lock.wait();
		}
		verify(chain, times(1)).invokeNextProcessor(any());
	}

	/** A runtime exception on a void method is dumped to logger but does not propagate exception to caller. */
	@Test
	public void GivenVoidMethodRuntimeException_WhenOnMethodInvocation_ThenDumpToLogger() throws Exception {
		// given
		when(managedMethod.isVoid()).thenReturn(true);
		when(chain.invokeNextProcessor(invocation)).thenThrow(RuntimeException.class);

		// when
		service.onMethodInvocation(chain, invocation);

		// then
	}

	@Test
	public void GivenFutureMethod_WhenOnMethodInvocation_ThenChainNextProcessor() throws Exception {
		// given
		when(chain.invokeNextProcessor(invocation)).thenReturn(new AsyncResult<String>("Tom Joad"));

		// when
		@SuppressWarnings("unchecked")
		Future<String> future = (Future<String>) service.onMethodInvocation(chain, invocation);

		// then
		assertThat(future.get(), equalTo("Tom Joad"));
		verify(chain, times(1)).invokeNextProcessor(any());
	}

	@Test
	public void GivenFutureMethodException_WhenOnMethodInvocation_ThenFutureGetException() throws Exception {
		// given
		when(chain.invokeNextProcessor(invocation)).thenThrow(new Exception("boom!"));

		// when
		@SuppressWarnings("unchecked")
		Future<String> future = (Future<String>) service.onMethodInvocation(chain, invocation);

		// then
		assertThat(future, notNullValue());

		String exception = null;
		try {
			future.get();
		} catch (Exception e) {
			exception = e.getMessage();
		}
		assertThat(exception, containsString("boom!"));
	}
}
