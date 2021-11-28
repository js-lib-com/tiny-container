package js.tiny.container.transaction;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import js.injector.IBindingBuilder;
import js.lang.InvocationException;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IInvocation;
import js.tiny.container.spi.IInvocationProcessorsChain;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.transaction.Mutable;
import js.transaction.Transaction;
import js.transaction.TransactionContext;
import js.transaction.Transactional;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceTest {
	@Mock
	private IContainer container;
	@Mock
	private IBindingBuilder<?> bindingBuilder;
	@Mock
	private IManagedClass<?> managedClass;
	@Mock
	private IManagedMethod managedMethod;

	@Mock
	private ITransactionalResource transactionalResource;
	@Mock
	private Transaction transaction;

	@Mock
	private Transactional transactional;
	@Mock
	private Transactional transactionalMeta;

	@Mock
	private IInvocationProcessorsChain processorsChain;
	@Mock
	IInvocation methodInvocation;

	private TransactionService service;

	@Before
	public void beforeTest() {
		doReturn(bindingBuilder).when(container).bind(any());
		doReturn(bindingBuilder).when(bindingBuilder).service();
		doReturn(bindingBuilder).when(bindingBuilder).to(any());
		doReturn(bindingBuilder).when(bindingBuilder).in(any());
		
		when(container.getInstance(TransactionContext.class)).thenReturn(transactionalResource);
		when(transactionalResource.createTransaction(null)).thenReturn(transaction);
		when(transactionalResource.createReadOnlyTransaction(null)).thenReturn(transaction);
		when(transaction.close()).thenReturn(true);

		doReturn(managedClass).when(managedMethod).getDeclaringClass();
		when(methodInvocation.method()).thenReturn(managedMethod);

		service = new TransactionService();
		service.create(container);
	}

	@Test
	public void GivenNotTransactionalMethod_WhenInvoke_ThenNextProcessor() throws Exception {
		// given

		// when
		service.onMethodInvocation(processorsChain, methodInvocation);

		// then
		verify(processorsChain, times(1)).invokeNextProcessor(methodInvocation);
		verify(container, times(0)).getInstance(ITransactionalResource.class);
	}

	@Test
	public void GivenImmutableTransaction_WhenInvoke_ThenNoCommitOrRollback() throws Exception {
		// given
		when(managedMethod.scanAnnotation(Transactional.class)).thenReturn(transactionalMeta);

		// when
		service.onMethodInvocation(processorsChain, methodInvocation);

		// then
		verify(transaction, times(0)).commit();
		verify(transaction, times(0)).rollback();
		verify(transaction, times(1)).close();
		verify(transactionalResource, times(1)).releaseResourceManager();
	}

	@Test
	public void GivenInheritedImmutableTransaction_WhenInvoke_ThenNoCommitOrRollback() throws Exception {
		// given
		when(managedClass.scanAnnotation(Transactional.class)).thenReturn(transactionalMeta);

		// when
		service.onMethodInvocation(processorsChain, methodInvocation);

		// then
		verify(transaction, times(0)).commit();
		verify(transaction, times(0)).rollback();
		verify(transaction, times(1)).close();
		verify(transactionalResource, times(1)).releaseResourceManager();
	}

	@Test
	public void GivenNestedImmutableTransaction_WhenInvoke_ThenNoResourceManagerRelease() throws Exception {
		// given
		when(managedMethod.scanAnnotation(Transactional.class)).thenReturn(transactionalMeta);
		when(transaction.close()).thenReturn(false);

		// when
		service.onMethodInvocation(processorsChain, methodInvocation);

		// then
		verify(transaction, times(0)).commit();
		verify(transaction, times(0)).rollback();
		verify(transaction, times(1)).close();
		verify(transactionalResource, times(0)).releaseResourceManager();
	}

	@Test
	public void GivenExceptionOnImmutableTransaction_WhenInvoke_ThenNoCommitOrRollback() throws Exception {
		// given
		when(managedMethod.scanAnnotation(Transactional.class)).thenReturn(transactionalMeta);
		when(processorsChain.invokeNextProcessor(methodInvocation)).thenThrow(IOException.class);

		// when
		try {
			service.onMethodInvocation(processorsChain, methodInvocation);
		} catch (InvocationException expected) {
		}

		// then
		verify(transaction, times(0)).commit();
		verify(transaction, times(0)).rollback();
		verify(transaction, times(1)).close();
		verify(transactionalResource, times(1)).releaseResourceManager();
	}

	@Test(expected = InvocationException.class)
	public void GivenExceptionOnImmutableTransaction_WhenInvoke_ThenInvocationException() throws Exception {
		// given
		when(managedMethod.scanAnnotation(Transactional.class)).thenReturn(transactionalMeta);
		when(processorsChain.invokeNextProcessor(methodInvocation)).thenThrow(IOException.class);

		// when
		service.onMethodInvocation(processorsChain, methodInvocation);

		// then
	}

	@Test
	public void GivenMutableTransaction_WhenInvoke_ThenCommit() throws Exception {
		// given
		when(managedMethod.scanAnnotation(Transactional.class)).thenReturn(transactionalMeta);
		when(managedMethod.scanAnnotation(Mutable.class)).thenReturn(Mockito.mock(Mutable.class));

		// when
		service.onMethodInvocation(processorsChain, methodInvocation);

		// then
		verify(transaction, times(1)).commit();
		verify(transaction, times(0)).rollback();
		verify(transaction, times(1)).close();
		verify(transactionalResource, times(1)).releaseResourceManager();
	}

	@Test
	public void GivenInheritedMutableTransaction_WhenInvoke_ThenCommit() throws Exception {
		// given
		when(managedClass.scanAnnotation(Transactional.class)).thenReturn(transactionalMeta);
		when(managedMethod.scanAnnotation(Mutable.class)).thenReturn(Mockito.mock(Mutable.class));

		// when
		service.onMethodInvocation(processorsChain, methodInvocation);

		// then
		verify(transaction, times(1)).commit();
		verify(transaction, times(0)).rollback();
		verify(transaction, times(1)).close();
		verify(transactionalResource, times(1)).releaseResourceManager();
	}

	@Test
	public void GivenNestedMutableTransaction_WhenInvoke_ThenNoResourceManagerRelease() throws Exception {
		// given
		when(managedMethod.scanAnnotation(Transactional.class)).thenReturn(transactionalMeta);
		when(managedMethod.scanAnnotation(Mutable.class)).thenReturn(Mockito.mock(Mutable.class));
		when(transaction.close()).thenReturn(false);

		// when
		service.onMethodInvocation(processorsChain, methodInvocation);

		// then
		verify(transaction, times(1)).commit();
		verify(transaction, times(0)).rollback();
		verify(transaction, times(1)).close();
		verify(transactionalResource, times(0)).releaseResourceManager();
	}

	@Test
	public void GivenExceptionOnMutableTransaction_WhenInvoke_ThenRollback() throws Exception {
		// given
		when(managedMethod.scanAnnotation(Transactional.class)).thenReturn(transactionalMeta);
		when(managedMethod.scanAnnotation(Mutable.class)).thenReturn(Mockito.mock(Mutable.class));
		when(processorsChain.invokeNextProcessor(methodInvocation)).thenThrow(IOException.class);

		// when
		try {
			service.onMethodInvocation(processorsChain, methodInvocation);
		} catch (InvocationException expected) {
		}

		// then
		verify(transaction, times(0)).commit();
		verify(transaction, times(1)).rollback();
		verify(transaction, times(1)).close();
		verify(transactionalResource, times(1)).releaseResourceManager();
	}

	@Test(expected = InvocationException.class)
	public void GivenExceptionOnMutableTransaction_WhenInvoke_ThenInvocationException() throws Exception {
		// given
		when(managedMethod.scanAnnotation(Transactional.class)).thenReturn(transactionalMeta);
		when(managedMethod.scanAnnotation(Mutable.class)).thenReturn(Mockito.mock(Mutable.class));
		when(processorsChain.invokeNextProcessor(methodInvocation)).thenThrow(IOException.class);

		// when
		service.onMethodInvocation(processorsChain, methodInvocation);

		// then
	}

	@Test
	public void Given_When_Then() {
		// given

		// when

		// then
	}
}
