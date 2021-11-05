package js.tiny.container.resource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collection;

import javax.annotation.Resource;
import javax.naming.Context;
import javax.naming.NamingException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.BugError;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IManagedClass;

@RunWith(MockitoJUnitRunner.class)
public class ResourcesInjectionProcessorTest {
	@Mock
	private IContainer container;
	@Mock
	private IManagedClass<BusinessClass> managedClass;

	@Mock
	private Context globalEnvironment;
	@Mock
	private Context componentEnvironment;

	private ResourcesInjectionProcessor processor;
	private BusinessClass instance;

	@Before
	public void beforeTest() throws NamingException {
		when(managedClass.getKey()).thenReturn(1);
		doReturn(BusinessClass.class).when(managedClass).getImplementationClass();

		// when(globalEnvironment.lookup(anyString())).thenThrow(NamingException.class);
		// when(componentEnvironment.lookup(anyString())).thenThrow(NamingException.class);

		processor = new ResourcesInjectionProcessor(globalEnvironment, componentEnvironment);
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
		assertThat(managedFields, hasItem(BusinessClass.field("resourceName")));
		assertThat(managedFields, hasItem(BusinessClass.field("resourceLookup")));
		assertThat(managedFields, not(hasItem(BusinessClass.field("unused"))));
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

	@Test
	public void GivenGlocalResource_WhenLookup_ThenFound() throws NamingException {
		// given
		when(componentEnvironment.lookup(anyString())).thenThrow(NamingException.class);
		when(globalEnvironment.lookup("resource.lookup")).thenReturn("resource");
		processor.scanServiceMeta(managedClass);

		// when
		processor.onInstancePostConstruction(managedClass, instance);

		// then
		assertThat(instance.resourceLookup, equalTo("resource"));
		assertThat(instance.resourceName, nullValue());
		assertThat(instance.resourceEmpty, nullValue());
	}

	@Test
	public void GivenNullInstance_WhenLookup_ThenNothing() throws NamingException {
		// given
		processor.scanServiceMeta(managedClass);

		// when
		processor.onInstancePostConstruction(managedClass, null);

		// then
	}

	@Test(expected = BugError.class)
	public void GivenGlocalResourceAndTypeMissmatch_WhenLookup_ThenException() throws NamingException {
		// given
		when(globalEnvironment.lookup("resource.lookup")).thenReturn(new Object());
		processor.scanServiceMeta(managedClass);

		// when
		processor.onInstancePostConstruction(managedClass, instance);

		// then
	}

	@Test
	public void GivenComponentResource_WhenLookup_ThenFound() throws NamingException {
		// given
		when(globalEnvironment.lookup(anyString())).thenThrow(NamingException.class);
		when(componentEnvironment.lookup(BusinessClass.resourceEmptyName())).thenThrow(NamingException.class);
		when(componentEnvironment.lookup("resource.name")).thenReturn("resource");
		processor.scanServiceMeta(managedClass);

		// when
		processor.onInstancePostConstruction(managedClass, instance);

		// then
		assertThat(instance.resourceName, equalTo("resource"));
		assertThat(instance.resourceLookup, nullValue());
		assertThat(instance.resourceEmpty, nullValue());
	}

	@Test(expected = BugError.class)
	public void GivenComponentResourceAndTypeMissmatch_WhenLookup_ThenException() throws NamingException {
		// given
		when(globalEnvironment.lookup(anyString())).thenThrow(NamingException.class);
		when(componentEnvironment.lookup("resource.name")).thenReturn(new Object());
		processor.scanServiceMeta(managedClass);

		// when
		processor.onInstancePostConstruction(managedClass, instance);

		// then
	}

	@Test
	public void GivenEmptyResource_WhenLookup_ThenFound() throws NamingException {
		// given
		when(globalEnvironment.lookup(anyString())).thenThrow(NamingException.class);
		when(componentEnvironment.lookup("resource.name")).thenThrow(NamingException.class);
		when(componentEnvironment.lookup(BusinessClass.resourceEmptyName())).thenReturn("resource");
		processor.scanServiceMeta(managedClass);

		// when
		processor.onInstancePostConstruction(managedClass, instance);

		// then
		assertThat(instance.resourceName, nullValue());
		assertThat(instance.resourceLookup, nullValue());
		assertThat(instance.resourceEmpty, equalTo("resource"));
	}

	// --------------------------------------------------------------------------------------------

	private static class BusinessClass {
		static Field field(String name) throws NoSuchFieldException, SecurityException {
			return BusinessClass.class.getDeclaredField(name);
		}

		static String resourceEmptyName() {
			return "js.tiny.container.resource.ResourcesInjectionProcessorTest$BusinessClass/resourceEmpty";
		}

		@Resource(lookup = "resource.lookup")
		String resourceLookup;

		@Resource(name = "resource.name")
		String resourceName;

		@Resource()
		String resourceEmpty;

		@SuppressWarnings("unused")
		String unused;
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
