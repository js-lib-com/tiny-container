package js.tiny.container.resource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.lang.reflect.Field;
import java.util.Collection;

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
public class FieldsCacheTest {
	@Mock
	private IContainer container;

	@Mock
	private Context globalEnvironment;
	@Mock
	private Context componentEnvironment;

	@Before
	public void beforeTest() throws NamingException {
	}

	@Test
	public void GivenDefaults_WhenScanFields_ThenFieldsInitialized() throws NoSuchFieldException, SecurityException {
		// given

		// when
		Collection<Field> managedFields = FieldsCache.scanFields(BusinessClass.class);

		// then
		assertThat(managedFields, notNullValue());
		assertThat(managedFields, hasItem(BusinessClass.field("resourceName")));
		assertThat(managedFields, hasItem(BusinessClass.field("resourceLookup")));
		assertThat(managedFields, not(hasItem(BusinessClass.field("unused"))));
	}

	@Test(expected = BugError.class)
	public void GivenStaticResource_WhenScanServiceMeta_ThenException() {
		// given

		// when
		FieldsCache.scanFields(StaticResource.class);

		// then
	}

	@Test(expected = BugError.class)
	public void GivenFinalResource_WhenScanServiceMeta_ThenException() {
		// given

		// when
		FieldsCache.scanFields(FinalResource.class);

		// then
	}

	// --------------------------------------------------------------------------------------------

	private static class BusinessClass {
		static Field field(String name) throws NoSuchFieldException, SecurityException {
			return BusinessClass.class.getDeclaredField(name);
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
