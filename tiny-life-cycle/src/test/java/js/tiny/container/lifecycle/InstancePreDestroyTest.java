package js.tiny.container.lifecycle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.annotation.PreDestroy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.BugError;
import js.lang.ManagedLifeCycle;
import js.lang.ManagedPreDestroy;
import js.tiny.container.lifecycle.InstancePreDestroyProcessor.Provider;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IInstancePreDestroyProcessor.Priority;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;

@RunWith(MockitoJUnitRunner.class)
public class InstancePreDestroyTest {
	@Mock
	private IContainer container;
	@Mock
	private IManagedClass<Object> managedClass;
	@Mock
	private IManagedMethod managedMethod;

	private InstancePreDestroyProcessor processor;

	@Before
	public void beforeTest() {
		doReturn(Object.class).when(managedClass).getImplementationClass();
		when(managedClass.getAttribute(any(), eq("pre-destroy"), eq(IManagedMethod.class))).thenReturn(managedMethod);

		InstancePreDestroyProcessor.Provider provider = new Provider();
		processor = provider.getService(container);
	}

	@Test
	public void GivenDefaults_WhenGetPriority_ThenDESTRUCTOR() {
		// given

		// when
		Priority priority = processor.getPriority();

		// then
		assertThat(priority, equalTo(Priority.DESTRUCTOR));
	}

	@Test
	public void GivenPreDestroyAnnotation_WhenScanMethod_ThenSetAttribute() {
		// given
		when(managedClass.getAttribute(any(), eq("pre-destroy"), eq(IManagedMethod.class))).thenReturn(null);
		when(managedMethod.getAnnotation(PreDestroy.class)).thenReturn(mock(PreDestroy.class));
		doReturn(managedClass).when(managedMethod).getDeclaringClass();

		// when
		processor.scanServiceMeta(managedMethod);

		// then
		verify(managedClass, times(1)).setAttribute(any(), eq("pre-destroy"), any());
	}

	@Test
	public void GivenMissingPreDestroyAnnotation_WhenScanMethod_ThenDoNotSetAttribute() {
		// given

		// when
		processor.scanServiceMeta(managedMethod);

		// then
		verify(managedClass, times(0)).setAttribute(any(), eq("pre-destroy"), any());
	}

	@Test(expected = BugError.class)
	public void GivenManagedService_WhenScanMethod_ThenException() {
		// given
		// managed class has already managed service attribute set on before test
		when(managedMethod.getAnnotation(PreDestroy.class)).thenReturn(mock(PreDestroy.class));
		doReturn(managedClass).when(managedMethod).getDeclaringClass();

		// when
		processor.scanServiceMeta(managedMethod);

		// then
	}

	@Test
	public void GivenManagedPreDestroy_WhenScanClass_ThenSetAttribute() {
		// given
		@SuppressWarnings("unchecked")
		IManagedClass<PreDestroyService> managedClass = mock(IManagedClass.class);
		doReturn(PreDestroyService.class).when(managedClass).getImplementationClass();

		// when
		processor.scanServiceMeta(managedClass);

		// then
		verify(managedClass, times(1)).setAttribute(any(), eq("pre-destroy"), any());
	}

	@Test
	public void GivenManagedLifeCycle_WhenScanClass_ThenSetAttribute() {
		// given
		@SuppressWarnings("unchecked")
		IManagedClass<LifeCycleService> managedClass = mock(IManagedClass.class);
		doReturn(LifeCycleService.class).when(managedClass).getImplementationClass();

		// when
		processor.scanServiceMeta(managedClass);

		// then
		verify(managedClass, times(1)).setAttribute(any(), eq("pre-destroy"), any());
	}

	@Test
	public void GivenMissingManagedPreDestroy_WhenScanClass_ThenDoNotSetAttribute() {
		// given

		// when
		processor.scanServiceMeta(managedClass);

		// then
		verify(managedClass, times(0)).setAttribute(any(), eq("pre-destroy"), any());
	}

	@Test
	public void GivenManagedMethod_WhenInstancePreDestroy_ThenInvoke() throws Exception {
		// given
		Object instance = new Object();

		// when
		processor.onInstancePreDestroy(managedClass, instance);

		// then
		verify(managedMethod, times(1)).invoke(instance);
	}

	@Test
	public void GivenManagedMethodFail_WhenInstancePreDestroy_ThenLogDump() throws Exception {
		// given
		Object instance = new Object();
		when(managedMethod.invoke(instance)).thenThrow(BugError.class);

		// when
		processor.onInstancePreDestroy(managedClass, instance);

		// then
	}

	@Test
	public void GivenMissingManagedMethod_WhenInstancePreDestroy_ThenNothing() throws Exception {
		// given
		when(managedClass.getAttribute(any(), eq("pre-destroy"), eq(IManagedMethod.class))).thenReturn(null);
		Object instance = new Object();

		// when
		processor.onInstancePreDestroy(managedClass, instance);

		// then
		verify(managedMethod, times(0)).invoke(instance);
	}

	@Test(expected = NullPointerException.class)
	public void GivenNullInstance_WhenInstancePreDestroy_ThenException() throws Exception {
		// given

		// when
		processor.onInstancePreDestroy(managedClass, null);

		// then
	}

	private static class PreDestroyService implements ManagedPreDestroy {
		@Override
		public void preDestroy() throws Exception {
		}
	}

	private static class LifeCycleService implements ManagedLifeCycle {
		@Override
		public void postConstruct() throws Exception {
		}

		@Override
		public void preDestroy() throws Exception {
		}
	}
}
