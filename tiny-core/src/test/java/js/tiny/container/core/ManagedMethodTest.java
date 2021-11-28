package js.tiny.container.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ejb.Asynchronous;
import javax.ejb.Remote;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.tiny.container.spi.IMethodInvocationProcessor;
import js.tiny.container.spi.IMethodInvocationProcessor.Priority;

@RunWith(MockitoJUnitRunner.class)
public class ManagedMethodTest {
	@Mock
	private ManagedClass<?> managedClass;
	@Mock
	private IMethodInvocationProcessor processor;

	private ManagedMethod managedMethod;

	@Before
	public void beforeTest() throws NoSuchMethodException, SecurityException {
		doReturn(Service.class).when(managedClass).getImplementationClass();
		doReturn(Service.class).when(managedClass).getInterfaceClass();

		when(processor.getPriority()).thenReturn(IMethodInvocationProcessor.Priority.ASYNCHRONOUS);
		managedMethod = new ManagedMethod(managedClass, Service.class.getMethod("task"));
	}

	@Test
	public void GivenImplementationAnnotation_WhenGetAnnotation_ThenNotNull() {
		// given

		// when
		Annotation annotation = managedMethod.scanAnnotation(Asynchronous.class);

		// then
		assertThat(annotation, notNullValue());
		verify(managedClass, times(0)).getInterfaceClass();
	}

	@Test
	public void GivenInterfaceAnnotation_WhenGetAnnotation_ThenLoadFromInterface() throws NoSuchMethodException, SecurityException {
		// given
		doReturn(IService.class).when(managedClass).getInterfaceClass();
		managedMethod = new ManagedMethod(managedClass, Service.class.getMethod("services", List.class));

		// when
		Annotation annotation = managedMethod.scanAnnotation(Asynchronous.class);

		// then
		assertThat(annotation, notNullValue());
		verify(managedClass, times(1)).getInterfaceClass();
	}

	@Test
	public void GivenMissingAnnotation_WhenGetAnnotation_ThenNullAndAttemptToLoadFromInterface() {
		// given

		// when
		Annotation annotation = managedMethod.scanAnnotation(Remote.class);

		// then
		assertThat(annotation, nullValue());
		verify(managedClass, times(1)).getInterfaceClass();
	}

	@Test
	public void GivenManagedMethod_WhenInvoke_ThenJavaMethodExecuted() throws Exception {
		// given
		Service instance = new Service();

		// when
		managedMethod.invoke(instance);

		// then
		assertThat(instance.taskInvocationProbe, equalTo(1));
	}

	@Test
	public void GivenProcessorBind_WhenInvoke_ThenInvokeProcessor() throws Exception {
		// given
		when(processor.bind(managedMethod)).thenReturn(true);
		managedMethod.scanServices(Arrays.asList(processor));
		Service instance = new Service();

		// when
		managedMethod.invoke(instance);

		// then
		verify(processor, times(1)).onMethodInvocation(any(), any());
		// processor is responsible for chaining method invocation
		// and since mock processor does not call invokeNextProcessor probe remains 0
		assertThat(instance.taskInvocationProbe, equalTo(0));
	}

	@Test
	public void GivenProcessorNotBind_WhenInvoke_ThenDoNotInvokeProcessor() throws Exception {
		// given
		when(processor.bind(managedMethod)).thenReturn(false);
		managedMethod.scanServices(Arrays.asList(processor));
		Service instance = new Service();

		// when
		managedMethod.invoke(instance);

		// then
		verify(processor, times(0)).onMethodInvocation(any(), any());
		assertThat(instance.taskInvocationProbe, equalTo(1));
	}

	/** Generic return type should be preserved at runtime. */
	@Test
	public void GivenGenericReturn_WhenGetReturnType_ThenTypeParameter() throws NoSuchMethodException, SecurityException {
		// given
		managedMethod = new ManagedMethod(managedClass, Service.class.getMethod("services", List.class));

		// when
		Type returnType = managedMethod.getReturnType();

		// then
		assertThat(returnType, instanceOf(ParameterizedType.class));

		ParameterizedType genericReturnType = (ParameterizedType) returnType;
		assertThat(genericReturnType.getRawType(), equalTo(List.class));
		assertThat(genericReturnType.getActualTypeArguments().length, equalTo(1));
		assertThat(genericReturnType.getActualTypeArguments()[0], equalTo(Service.class));
	}

	/** Generic invocation argument type should be preserved at runtime. */
	@Test
	public void GivenGenericArgument_WhenGetParameterTypes_ThenTypeParameter() throws NoSuchMethodException, SecurityException {
		// given
		managedMethod = new ManagedMethod(managedClass, Service.class.getMethod("services", List.class));

		// when
		Type[] parameterTypes = managedMethod.getParameterTypes();

		// then
		assertThat(parameterTypes.length, equalTo(1));
		assertThat(parameterTypes[0], instanceOf(ParameterizedType.class));

		ParameterizedType genericArgumentType = (ParameterizedType) parameterTypes[0];
		assertThat(genericArgumentType.getRawType(), equalTo(List.class));
		assertThat(genericArgumentType.getActualTypeArguments().length, equalTo(1));
		assertThat(genericArgumentType.getActualTypeArguments()[0], equalTo(Service.class));
	}

	@Test
	public void GivenManagedMethod_WhenGetName_ThenShortMethodName() {
		// given

		// when
		String name = managedMethod.getName();

		// then
		assertThat(name, equalTo("task"));
	}

	@Test
	public void GivenManagedMethod_WhenGetSignature_ThenIncludeImplementationNameAndParameters() throws NoSuchMethodException, SecurityException {
		// given
		managedMethod = new ManagedMethod(managedClass, Service.class.getMethod("services", List.class));

		// when
		String signature = managedMethod.getSignature();

		// then
		assertThat(signature, containsString("Service#services(List)"));
	}

	/**
	 * Managed method is part of method invocation chain and should have last priority in order to be executed after all
	 * processors.
	 */
	@Test
	public void GivenManagedMethod_WhenGetPriority_ThenMETHODisLast() {
		// given

		// when
		Priority priority = managedMethod.getPriority();

		// then
		assertThat(priority.ordinal(), equalTo(Priority.values().length - 1));
	}

	/** Equals is based o wrapped Java method. Two managed method instances wrapping the same Java method should be equal. */
	@Test
	public void GivenAnotherInstanceOnSameJavaMethod_WhenEquals_ThenTrue() throws NoSuchMethodException, SecurityException {
		// given
		ManagedMethod managedMethod2 = new ManagedMethod(managedClass, Service.class.getMethod("task"));

		// when
		boolean equals = managedMethod.equals(managedMethod2);

		// then
		assertThat(equals, is(true));
	}

	/**
	 * Hash code is based o wrapped Java method. Two managed method instances wrapping the same Java method should have the same
	 * hash code.
	 */
	@Test
	public void GivenAnotherInstanceOnSameJavaMethod_WhenHashCode_ThenEqual() throws NoSuchMethodException, SecurityException {
		// given
		ManagedMethod managedMethod2 = new ManagedMethod(managedClass, Service.class.getMethod("task"));

		// when
		int hashCode1 = managedMethod.hashCode();
		int hashCode2 = managedMethod2.hashCode();

		// then
		assertThat(hashCode1, equalTo(hashCode2));
	}

	// --------------------------------------------------------------------------------------------

	private interface IService {
		@Asynchronous
		List<Service> services(List<Service> services);
	}

	private static class Service implements IService {
		int taskInvocationProbe;

		@Asynchronous
		public void task() {
			++taskInvocationProbe;
		}

		public List<Service> services(List<Service> services) {
			return Collections.emptyList();
		}
	}
}
