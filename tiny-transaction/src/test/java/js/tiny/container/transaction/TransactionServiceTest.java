package js.tiny.container.transaction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.InvocationException;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IServiceMeta;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IMethodInvocation;
import js.tiny.container.spi.IMethodInvocationProcessorsChain;
import js.transaction.Transaction;
import js.transaction.Transactional;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceTest {
	@Mock
	private IContainer container;
	@Mock
	private IManagedClass managedClass;
	@Mock
	private IManagedMethod managedMethod;

	@Mock
	private ITransactionalResource transactionalResource;
	@Mock
	private Transaction transaction;

	@Mock
	private Transactional transactional;
	@Mock
	private TransactionalMeta transactionalMeta;

	@Mock
	private IMethodInvocationProcessorsChain processorsChain;
	@Mock
	IMethodInvocation methodInvocation;

	private TransactionService service;

	@Before
	public void beforeTest() {
		when(container.getInstance(ITransactionalResource.class)).thenReturn(transactionalResource);
		when(transactionalResource.createTransaction(null)).thenReturn(transaction);
		when(transactionalResource.createReadOnlyTransaction(null)).thenReturn(transaction);
		when(transaction.close()).thenReturn(true);

		when(managedMethod.getDeclaringClass()).thenReturn(managedClass);
		when(transactional.schema()).thenReturn("");
		when(methodInvocation.method()).thenReturn(managedMethod);

		service = new TransactionService(container);
	}

	@Test
	public void GivenTransactionalClass_WhenScanClass_ThenServicesMeta() {
		// given
		when(managedClass.getAnnotation(Transactional.class)).thenReturn(transactional);

		// when
		List<IServiceMeta> servicesMeta = service.scan(managedClass);

		// then
		assertThat(servicesMeta, not(empty()));
	}

	@Test
	public void GivenNotTransactionalClass_WhenScanClass_ThenEmptyServicesMeta() {
		// given
		when(managedClass.getAnnotation(Transactional.class)).thenReturn(transactional);

		// when
		List<IServiceMeta> servicesMeta = service.scan(managedClass);

		// then
		assertThat(servicesMeta, not(empty()));
	}

	@Test
	public void GivenTransactionalMethod_WhenScanMethod_ThenServicesMeta() {
		// given
		when(managedMethod.getAnnotation(Transactional.class)).thenReturn(transactional);

		// when
		List<IServiceMeta> servicesMeta = service.scan(managedMethod);

		// then
		assertThat(servicesMeta, not(empty()));
	}

	@Test
	public void GivenNotTransactionalMethod_WhenScanMethod_ThenEmptyServicesMeta() {
		// given
		when(managedMethod.getAnnotation(Transactional.class)).thenReturn(transactional);

		// when
		List<IServiceMeta> servicesMeta = service.scan(managedMethod);

		// then
		assertThat(servicesMeta, not(empty()));
	}

	@Test
	public void GivenNotTransactionalMethod_WhenInvoke_ThenNextProcessor() throws IllegalArgumentException, InvocationException, AuthorizationException {
		// given

		// when
		service.invoke(processorsChain, methodInvocation);

		// then
		verify(processorsChain, times(1)).invokeNextProcessor(methodInvocation);
		verify(container, times(0)).getInstance(ITransactionalResource.class);
	}

	@Test
	public void GivenImmutableTransaction_WhenInvoke_ThenNoCommitOrRollback() throws IllegalArgumentException, InvocationException, AuthorizationException {
		// given
		when(managedMethod.getServiceMeta(TransactionalMeta.class)).thenReturn(transactionalMeta);

		// when
		service.invoke(processorsChain, methodInvocation);

		// then
		verify(transaction, times(0)).commit();
		verify(transaction, times(0)).rollback();
		verify(transaction, times(1)).close();
		verify(transactionalResource, times(1)).releaseResourceManager();
	}

	@Test
	public void GivenInheritedImmutableTransaction_WhenInvoke_ThenNoCommitOrRollback() throws IllegalArgumentException, InvocationException, AuthorizationException {
		// given
		when(managedClass.getServiceMeta(TransactionalMeta.class)).thenReturn(transactionalMeta);

		// when
		service.invoke(processorsChain, methodInvocation);

		// then
		verify(transaction, times(0)).commit();
		verify(transaction, times(0)).rollback();
		verify(transaction, times(1)).close();
		verify(transactionalResource, times(1)).releaseResourceManager();
	}

	@Test
	public void GivenNestedImmutableTransaction_WhenInvoke_ThenNoResourceManagerRelease() throws IllegalArgumentException, InvocationException, AuthorizationException {
		// given
		when(managedMethod.getServiceMeta(TransactionalMeta.class)).thenReturn(transactionalMeta);
		when(transaction.close()).thenReturn(false);

		// when
		service.invoke(processorsChain, methodInvocation);

		// then
		verify(transaction, times(0)).commit();
		verify(transaction, times(0)).rollback();
		verify(transaction, times(1)).close();
		verify(transactionalResource, times(0)).releaseResourceManager();
	}

	@Test
	public void GivenExceptionOnImmutableTransaction_WhenInvoke_ThenNoCommitOrRollback() throws IllegalArgumentException, InvocationException, AuthorizationException {
		// given
		when(managedMethod.getServiceMeta(TransactionalMeta.class)).thenReturn(transactionalMeta);
		when(processorsChain.invokeNextProcessor(methodInvocation)).thenThrow(IOException.class);

		// when
		try {
			service.invoke(processorsChain, methodInvocation);
		} catch (InvocationException expected) {
		}

		// then
		verify(transaction, times(0)).commit();
		verify(transaction, times(0)).rollback();
		verify(transaction, times(1)).close();
		verify(transactionalResource, times(1)).releaseResourceManager();
	}

	@Test(expected = InvocationException.class)
	public void GivenExceptionOnImmutableTransaction_WhenInvoke_ThenInvocationException() throws IllegalArgumentException, InvocationException, AuthorizationException {
		// given
		when(managedMethod.getServiceMeta(TransactionalMeta.class)).thenReturn(transactionalMeta);
		when(processorsChain.invokeNextProcessor(methodInvocation)).thenThrow(IOException.class);

		// when
		service.invoke(processorsChain, methodInvocation);

		// then
	}

	@Test
	public void GivenMutableTransaction_WhenInvoke_ThenCommit() throws IllegalArgumentException, InvocationException, AuthorizationException {
		// given
		when(managedMethod.getServiceMeta(TransactionalMeta.class)).thenReturn(transactionalMeta);
		when(managedMethod.getServiceMeta(MutableMeta.class)).thenReturn(Mockito.mock(MutableMeta.class));

		// when
		service.invoke(processorsChain, methodInvocation);

		// then
		verify(transaction, times(1)).commit();
		verify(transaction, times(0)).rollback();
		verify(transaction, times(1)).close();
		verify(transactionalResource, times(1)).releaseResourceManager();
	}

	@Test
	public void GivenInheritedMutableTransaction_WhenInvoke_ThenCommit() throws IllegalArgumentException, InvocationException, AuthorizationException {
		// given
		when(managedClass.getServiceMeta(TransactionalMeta.class)).thenReturn(transactionalMeta);
		when(managedMethod.getServiceMeta(MutableMeta.class)).thenReturn(Mockito.mock(MutableMeta.class));

		// when
		service.invoke(processorsChain, methodInvocation);

		// then
		verify(transaction, times(1)).commit();
		verify(transaction, times(0)).rollback();
		verify(transaction, times(1)).close();
		verify(transactionalResource, times(1)).releaseResourceManager();
	}

	@Test
	public void GivenNestedMutableTransaction_WhenInvoke_ThenNoResourceManagerRelease() throws IllegalArgumentException, InvocationException, AuthorizationException {
		// given
		when(managedMethod.getServiceMeta(TransactionalMeta.class)).thenReturn(transactionalMeta);
		when(managedMethod.getServiceMeta(MutableMeta.class)).thenReturn(Mockito.mock(MutableMeta.class));
		when(transaction.close()).thenReturn(false);

		// when
		service.invoke(processorsChain, methodInvocation);

		// then
		verify(transaction, times(1)).commit();
		verify(transaction, times(0)).rollback();
		verify(transaction, times(1)).close();
		verify(transactionalResource, times(0)).releaseResourceManager();
	}

	@Test
	public void GivenExceptionOnMutableTransaction_WhenInvoke_ThenRollback() throws IllegalArgumentException, InvocationException, AuthorizationException {
		// given
		when(managedMethod.getServiceMeta(TransactionalMeta.class)).thenReturn(transactionalMeta);
		when(managedMethod.getServiceMeta(MutableMeta.class)).thenReturn(Mockito.mock(MutableMeta.class));
		when(processorsChain.invokeNextProcessor(methodInvocation)).thenThrow(IOException.class);

		// when
		try {
			service.invoke(processorsChain, methodInvocation);
		} catch (InvocationException expected) {
		}

		// then
		verify(transaction, times(0)).commit();
		verify(transaction, times(1)).rollback();
		verify(transaction, times(1)).close();
		verify(transactionalResource, times(1)).releaseResourceManager();
	}

	@Test(expected = InvocationException.class)
	public void GivenExceptionOnMutableTransaction_WhenInvoke_ThenInvocationException() throws IllegalArgumentException, InvocationException, AuthorizationException {
		// given
		when(managedMethod.getServiceMeta(TransactionalMeta.class)).thenReturn(transactionalMeta);
		when(managedMethod.getServiceMeta(MutableMeta.class)).thenReturn(Mockito.mock(MutableMeta.class));
		when(processorsChain.invokeNextProcessor(methodInvocation)).thenThrow(IOException.class);

		// when
		service.invoke(processorsChain, methodInvocation);

		// then
	}

	@Test
	public void Given_When_Then() {
		// given

		// when

		// then
	}
}
