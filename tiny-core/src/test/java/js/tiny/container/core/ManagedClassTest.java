package js.tiny.container.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Iterator;

import javax.inject.Singleton;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.tiny.container.cdi.IClassBinding;
import js.tiny.container.spi.IClassPostLoadedProcessor;
import js.tiny.container.spi.IFlowProcessor;
import js.tiny.container.spi.IInstancePostConstructProcessor;
import js.tiny.container.spi.IInstancePreDestroyProcessor;
import js.tiny.container.spi.IManagedMethod;

@RunWith(MockitoJUnitRunner.class)
public class ManagedClassTest {
	@Mock
	private Container container;
	@Mock
	private IClassPostLoadedProcessor postLoadedProcessor;
	@Mock
	private IInstancePostConstructProcessor postConstructorProcessor;
	@Mock
	private IInstancePreDestroyProcessor preDestroyProcessor;
	@Mock
	private IClassBinding<Object> binding;

	private ManagedClass<Object> managedClass;

	@Before
	public void beforeTest() {
		doReturn(Object.class).when(binding).getInterfaceClass();
		doReturn(Object.class).when(binding).getImplementationClass();
		managedClass = new ManagedClass<>(container, binding);

		when(postConstructorProcessor.getPriority()).thenReturn(IInstancePostConstructProcessor.Priority.CONSTRUCTOR);
		when(postConstructorProcessor.bind(managedClass)).thenReturn(true);

		when(preDestroyProcessor.getPriority()).thenReturn(IInstancePreDestroyProcessor.Priority.DESTRUCTOR);
		when(preDestroyProcessor.bind(managedClass)).thenReturn(true);
	}

	/** Class post loaded processors are executed on managed class service scan. */
	@Test
	public void GivenIClassPostLoadedProcessor_WhenScanServices_ThenExecuteProcessor() {
		// given
		when(container.getServices()).thenReturn(Arrays.asList(postLoadedProcessor));

		// when
		managedClass.scanServices();

		// then
		verify(postLoadedProcessor, times(1)).onClassPostLoaded(managedClass);
	}

	@Test
	public void GivenIInstancePostConstructProcessorBind_WhenScanServices_ThenContains() {
		// given
		when(container.getServices()).thenReturn(Arrays.asList(postConstructorProcessor));

		// when
		managedClass.scanServices();

		// then
		assertThat(managedClass.instancePostConstructors(), contains(postConstructorProcessor));
	}

	@Test
	public void GivenIInstancePostConstructProcessorNotBind_WhenScanServices_ThenNotContains() {
		// given
		when(container.getServices()).thenReturn(Arrays.asList(postConstructorProcessor));
		when(postConstructorProcessor.bind(managedClass)).thenReturn(false);

		// when
		managedClass.scanServices();

		// then
		assertThat(managedClass.instancePostConstructors(), not(contains(postConstructorProcessor)));
	}

	@Test
	public void GiveIInstancePreDestroyProcessorBind_WhenScanServices_ThenContains() {
		// given
		when(container.getServices()).thenReturn(Arrays.asList(preDestroyProcessor));

		// when
		managedClass.scanServices();

		// then
		assertThat(managedClass.instancePreDestructors(), contains(preDestroyProcessor));
	}

	@Test
	public void GiveIInstancePreDestroyProcessorNotBind_WhenScanServices_ThenNotContains() {
		// given
		when(container.getServices()).thenReturn(Arrays.asList(preDestroyProcessor));
		when(preDestroyProcessor.bind(managedClass)).thenReturn(false);

		// when
		managedClass.scanServices();

		// then
		assertThat(managedClass.instancePreDestructors(), not(contains(preDestroyProcessor)));
	}

	private static <P extends IFlowProcessor> Matcher<FlowProcessorsSet<P>> contains(P processorToMatch) {
		return new TypeSafeDiagnosingMatcher<FlowProcessorsSet<P>>() {
			@Override
			public void describeTo(Description description) {
				description.appendText(processorToMatch.toString());
			}

			@Override
			protected boolean matchesSafely(FlowProcessorsSet<P> processors, Description mismatchDescription) {
				Iterator<P> iterator = processors.iterator();
				while (iterator.hasNext()) {
					if (iterator.next().equals(processorToMatch)) {
						return true;
					}
				}
				return false;
			}
		};
	}

	@Test
	public void GivenExistingProcessor_WhenOnInstanceCreated_ThenExecute() {
		// given
		when(container.getServices()).thenReturn(Arrays.asList(postConstructorProcessor));
		managedClass.scanServices();

		Object instance = new Object();

		// when
		managedClass.onInstanceCreated(instance);

		// then
		verify(postConstructorProcessor, times(1)).onInstancePostConstruct(instance);
	}

	@Test
	public void GivenManagedClass_WhenGetInstance_ThenDelegateContainer() {
		// given

		// when
		managedClass.getInstance();

		// then
		verify(container, times(1)).getInstance(Object.class);
	}

	@Test
	public void GivenInterfaceAnnotation_WhenGetAnnotation_ThenNotNull() {
		// given
		@SuppressWarnings("unchecked")
		IClassBinding<IService1> binding = mock(IClassBinding.class);
		doReturn(IService1.class).when(binding).getInterfaceClass();
		doReturn(Service1.class).when(binding).getImplementationClass();

		ManagedClass<IService1> managedClass = new ManagedClass<>(container, binding);

		// when
		Annotation annotation = managedClass.scanAnnotation(Singleton.class);

		// then
		assertThat(annotation, notNullValue());
	}

	@Test
	public void GivenImplementationAnnotation_WhenGetAnnotation_ThenNotNull() {
		// given
		@SuppressWarnings("unchecked")
		IClassBinding<IService2> binding = mock(IClassBinding.class);
		doReturn(IService2.class).when(binding).getInterfaceClass();
		doReturn(Service2.class).when(binding).getImplementationClass();

		ManagedClass<IService2> managedClass = new ManagedClass<>(container, binding);

		// when
		Annotation annotation = managedClass.scanAnnotation(Singleton.class);

		// then
		assertThat(annotation, notNullValue());
	}

	@Test
	public void GivenMissingAnnotation_WhenGetAnnotation_ThenNull() {
		// given

		// when
		Annotation annotation = managedClass.scanAnnotation(Singleton.class);

		// then
		assertThat(annotation, nullValue());
	}

	@Test
	public void GivenExistingMethod_WhenGetManagedMethod_ThenNotNull() {
		// given
		@SuppressWarnings("unchecked")
		IClassBinding<IService1> binding = mock(IClassBinding.class);
		doReturn(IService1.class).when(binding).getInterfaceClass();
		doReturn(Service1.class).when(binding).getImplementationClass();

		ManagedClass<IService1> managedClass = new ManagedClass<IService1>(container, binding);
		managedClass.scanServices();

		// when
		IManagedMethod managedMethod = managedClass.getManagedMethod("execute");

		// then
		assertThat(managedMethod, notNullValue());
	}

	@Test
	public void GivenMissingMethod_WhenGetManagedMethod_ThenNullValue() {
		// given

		// when
		IManagedMethod managedMethod = managedClass.getManagedMethod("fake");

		// then
		assertThat(managedMethod, nullValue());
	}

	// --------------------------------------------------------------------------------------------

	@Singleton
	private static interface IService1 {
		void execute();
	}

	private static class Service1 implements IService1 {
		public void execute() {
		}
	}

	private static interface IService2 {
	}

	@Singleton
	private static class Service2 implements IService2 {
	}
}
