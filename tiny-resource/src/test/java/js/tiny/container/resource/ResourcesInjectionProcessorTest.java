package js.tiny.container.resource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;

import javax.naming.Context;
import javax.naming.NamingException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import jakarta.annotation.Resource;
import js.lang.BugError;
import js.tiny.container.spi.IContainer;

@RunWith(MockitoJUnitRunner.class)
public class ResourcesInjectionProcessorTest {
	@Mock
	private IContainer container;

	@Mock
	private Context globalEnvironment;
	@Mock
	private Context componentEnvironment;
	@Mock
	private FieldsCache fieldsCache;

	private ResourcesInjectionProcessor processor;
	private BusinessClass instance;

	@Before
	public void beforeTest() throws NamingException {
		processor = new ResourcesInjectionProcessor(globalEnvironment, componentEnvironment, fieldsCache);
		instance = new BusinessClass();
	}

	@Test
	public void GivenGlocalResource_WhenLookup_ThenFound() throws NamingException {
		// given
		when(globalEnvironment.lookup("resource.lookup")).thenReturn("resource");
		when(fieldsCache.get(BusinessClass.class)).thenReturn(Arrays.asList(BusinessClass.field("resourceLookup")));

		// when
		processor.onInstancePostConstruct(instance);

		// then
		assertThat(instance.resourceLookup, equalTo("resource"));
		assertThat(instance.resourceName, nullValue());
		assertThat(instance.resourceEmpty, nullValue());
	}

	@Test
	public void GivenNullInstance_WhenLookup_ThenNothing() throws NamingException {
		// given

		// when
		processor.onInstancePostConstruct(null);

		// then
	}

	@Test(expected = BugError.class)
	public void GivenGlocalResourceAndTypeMissmatch_WhenLookup_ThenException() throws NamingException {
		// given
		when(globalEnvironment.lookup("resource.lookup")).thenReturn(new Object());
		when(fieldsCache.get(BusinessClass.class)).thenReturn(Arrays.asList(BusinessClass.field("resourceLookup")));

		// when
		processor.onInstancePostConstruct(instance);

		// then
	}

	@Test
	public void GivenComponentResource_WhenLookup_ThenFound() throws NamingException {
		// given
		when(componentEnvironment.lookup("resource.name")).thenReturn("resource");
		when(fieldsCache.get(BusinessClass.class)).thenReturn(Arrays.asList(BusinessClass.field("resourceName")));

		// when
		processor.onInstancePostConstruct(instance);

		// then
		assertThat(instance.resourceName, equalTo("resource"));
		assertThat(instance.resourceLookup, nullValue());
		assertThat(instance.resourceEmpty, nullValue());
	}

	@Test(expected = BugError.class)
	public void GivenComponentResourceAndTypeMissmatch_WhenLookup_ThenException() throws NamingException {
		// given
		when(componentEnvironment.lookup("resource.name")).thenReturn(new Object());
		when(fieldsCache.get(BusinessClass.class)).thenReturn(Arrays.asList(BusinessClass.field("resourceName")));

		// when
		processor.onInstancePostConstruct(instance);

		// then
	}

	@Test
	public void GivenEmptyResource_WhenLookup_ThenFound() throws NamingException {
		// given
		when(componentEnvironment.lookup(BusinessClass.resourceEmptyName())).thenReturn("resource");
		when(fieldsCache.get(BusinessClass.class)).thenReturn(Arrays.asList(BusinessClass.field("resourceEmpty")));

		// when
		processor.onInstancePostConstruct(instance);

		// then
		assertThat(instance.resourceName, nullValue());
		assertThat(instance.resourceLookup, nullValue());
		assertThat(instance.resourceEmpty, equalTo("resource"));
	}

	// --------------------------------------------------------------------------------------------

	private static class BusinessClass {
		static Field field(String name) {
			try {
				return BusinessClass.class.getDeclaredField(name);
			} catch (NoSuchFieldException | SecurityException e) {
				// silently ignore not possible error conditions
				return null;
			}
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
	}
}
