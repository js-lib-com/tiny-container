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

import javax.annotation.PostConstruct;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.BugError;
import js.lang.ManagedLifeCycle;
import js.lang.ManagedPostConstruct;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IInstancePostConstructProcessor.Priority;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;

@RunWith(MockitoJUnitRunner.class)
public class InstancePostConstructTest {
	@Mock
	private IContainer container;
	@Mock
	private IManagedClass<Object> managedClass;
	@Mock
	private IManagedMethod managedMethod;

	private InstancePostConstructProcessor processor;

	@Before
	public void beforeTest() {
		doReturn(Object.class).when(managedClass).getImplementationClass();
		when(managedClass.getAttribute(any(), eq("post-construct"), eq(IManagedMethod.class))).thenReturn(managedMethod);

		processor = new InstancePostConstructProcessor();
	}

	@Test
	public void GivenDefaults_WhenGetPriority_ThenCONSTRCUTOR() {
		// given

		// when
		Priority priority = processor.getPriority();

		// then
		assertThat(priority, equalTo(Priority.CONSTRUCTOR));
	}

	@Test
	public void GivenPostConstructAnnotation_WhenScanMethod_ThenSetAttribute() {
		// given
		when(managedClass.getAttribute(any(), eq("post-construct"), eq(IManagedMethod.class))).thenReturn(null);
		when(managedMethod.scanAnnotation(PostConstruct.class)).thenReturn(mock(PostConstruct.class));
		doReturn(managedClass).when(managedMethod).getDeclaringClass();

		// when
		processor.scanMethodAnnotations(managedMethod);

		// then
		verify(managedClass, times(1)).setAttribute(any(), eq("post-construct"), any());
	}

	@Test
	public void GivenMissingPostConstructAnnotation_WhenScanMethod_ThenDoNotSetAttribute() {
		// given

		// when
		processor.scanMethodAnnotations(managedMethod);

		// then
		verify(managedClass, times(0)).setAttribute(any(), eq("post-construct"), any());
	}

	@Test(expected = BugError.class)
	public void GivenManagedService_WhenScanMethod_ThenException() {
		// given
		// managed class has already managed service attribute set on before test
		when(managedMethod.scanAnnotation(PostConstruct.class)).thenReturn(mock(PostConstruct.class));
		doReturn(managedClass).when(managedMethod).getDeclaringClass();

		// when
		processor.scanMethodAnnotations(managedMethod);

		// then
	}

	@Test
	public void GivenManagedPostConstruct_WhenScanClass_ThenSetAttribute() {
		// given
		@SuppressWarnings("unchecked")
		IManagedClass<PostConstructService> managedClass = mock(IManagedClass.class);
		doReturn(PostConstructService.class).when(managedClass).getImplementationClass();

		// when
		processor.scanClassAnnotations(managedClass);

		// then
		verify(managedClass, times(1)).setAttribute(any(), eq("post-construct"), any());
	}

	@Test
	public void GivenManagedLifeCycle_WhenScanClass_ThenSetAttribute() {
		// given
		@SuppressWarnings("unchecked")
		IManagedClass<LifeCycleService> managedClass = mock(IManagedClass.class);
		doReturn(LifeCycleService.class).when(managedClass).getImplementationClass();

		// when
		processor.scanClassAnnotations(managedClass);

		// then
		verify(managedClass, times(1)).setAttribute(any(), eq("post-construct"), any());
	}

	@Test
	public void GivenMissingManagedPostConstruct_WhenScanClass_ThenDoNotSetAttribute() {
		// given

		// when
		processor.scanClassAnnotations(managedClass);

		// then
		verify(managedClass, times(0)).setAttribute(any(), eq("post-construct"), any());
	}

	@Test
	public void GivenManagedMethod_WhenOnInstancePostConstruct_ThenInvoke() throws Exception {
		// given
		Object instance = new Object();

		// when
		processor.onInstancePostConstruct(managedClass, instance);

		// then
		verify(managedMethod, times(1)).invoke(instance);
	}

	@Test(expected = BugError.class)
	public void GivenManagedMethodFail_WhenOnInstancePostConstruct_ThenException() throws Exception {
		// given
		Object instance = new Object();
		when(managedMethod.invoke(instance)).thenThrow(BugError.class);

		// when
		processor.onInstancePostConstruct(managedClass, instance);

		// then
	}

	@Test
	public void GivenMissingManagedMethod_WhenOnInstancePostConstruct_ThenNothing() throws Exception {
		// given
		when(managedClass.getAttribute(any(), eq("post-construct"), eq(IManagedMethod.class))).thenReturn(null);
		Object instance = new Object();

		// when
		processor.onInstancePostConstruct(managedClass, instance);

		// then
		verify(managedMethod, times(0)).invoke(instance);
	}

	@Test(expected = NullPointerException.class)
	public void GivenNullInstance_WhenOnInstancePostConstruct_ThenException() throws Exception {
		// given

		// when
		processor.onInstancePostConstruct(managedClass, null);

		// then
	}

	private static class PostConstructService implements ManagedPostConstruct {
		@Override
		public void postConstruct() throws Exception {
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
