package js.tiny.container.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collection;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.BugError;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IManagedClass;

@RunWith(MockitoJUnitRunner.class)
public class InstanceFieldsInjectionProcessorTest {
	@Mock
	private IContainer container;
	@Mock
	private IManagedClass<BusinessClass> managedClass;

	private InstanceFieldsInjectionProcessor processor;
	private BusinessClass instance;

	@Before
	public void beforeTest() {
		when(container.getOptionalInstance(Dependency.class)).thenReturn(new Dependency());

		when(managedClass.getContainer()).thenReturn(container);
		when(managedClass.getKey()).thenReturn(1);
		doReturn(BusinessClass.class).when(managedClass).getImplementationClass();

		processor = new InstanceFieldsInjectionProcessor();
		instance = new BusinessClass();
	}

	@Test
	public void GivenDefaults_WhenScanServiceMeta_ThenFields() throws NoSuchFieldException, SecurityException {
		// given

		// when
		processor.scanServiceMeta(managedClass);

		// then
		Collection<Field> managedFields = processor.getManagedFields(1);
		assertThat(managedFields, notNullValue());
		assertThat(managedFields, hasItem(BusinessClass.class.getDeclaredField("inject")));
		assertThat(managedFields, hasItem(BusinessClass.class.getDeclaredField("resourceName")));
		assertThat(managedFields, hasItem(BusinessClass.class.getDeclaredField("resourceLookup")));
		assertThat(managedFields, not(hasItem(BusinessClass.class.getDeclaredField("unused"))));
	}

	@Test
	public void GivenInstanceFieldInject_WhenExecute_ThenNotNullValue() {
		// given
		processor.scanServiceMeta(managedClass);

		// when
		processor.onInstancePostConstruction(managedClass, instance);

		// then
		assertThat(instance.inject, notNullValue());
	}

	@Test(expected = BugError.class)
	public void GivenStaticInject_WhenScanServiceMeta_ThenException() {
		// given
		doReturn(StaticInject.class).when(managedClass).getImplementationClass();

		// when
		processor.scanServiceMeta(managedClass);

		// then
	}

	@Test(expected = BugError.class)
	public void GivenFinalInject_WhenScanServiceMeta_ThenException() {
		// given
		doReturn(FinalInject.class).when(managedClass).getImplementationClass();

		// when
		processor.scanServiceMeta(managedClass);

		// then
	}

	@Test(expected = BugError.class)
	public void GivenStaticResource_WhenScanServiceMeta_ThenException() {
		// given
		doReturn(StaticResource.class).when(managedClass).getImplementationClass();

		// when
		processor.scanServiceMeta(managedClass);

		// then
	}

	@Test(expected = BugError.class)
	public void GivenFinalResource_WhenScanServiceMeta_ThenException() {
		// given
		doReturn(FinalResource.class).when(managedClass).getImplementationClass();

		// when
		processor.scanServiceMeta(managedClass);

		// then
	}

	private static class BusinessClass {
		@Inject
		Dependency inject;

		@Resource(name = "resource.name")
		String resourceName;

		@Resource(name = "resource.lookup")
		String resourceLookup;
		
		@SuppressWarnings("unused")
		String unused; 
	}

	private static class Dependency {

	}

	private static class StaticInject {
		@SuppressWarnings("unused")
		@Inject
		static Dependency staticInject;
	}

	private static class FinalInject {
		@SuppressWarnings("unused")
		@Inject
		final Dependency finalInject = null;
	}

	private static class StaticResource {
		@Resource(name = "static.resource")
		static String staticResource;
	}

	private static class FinalResource {
		@Resource(name = "final.resource")
		final String finalResource = null;
	}
}
