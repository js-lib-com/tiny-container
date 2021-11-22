package js.tiny.container.cdi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.injector.ProvisionException;

import js.tiny.container.fixture.IService;

@RunWith(MockitoJUnitRunner.class)
public class ServiceProviderTest {
	private ServiceProvider<IService> service;

	@Before
	public void beforeTest() {
		service = new ServiceProvider<>(IService.class);
	}

	@Test
	public void GivenExistingService_WhenGet_ThenNotNull() {
		// given

		// when
		IService instance = service.get();

		// then
		assertThat(instance, notNullValue());
		assertThat(instance.name(), equalTo("service"));
	}

	@Test(expected = ProvisionException.class)
	public void GivenMissingService_WhenGet_ThenException() {
		// given
		ServiceProvider<Object> service = new ServiceProvider<Object>(Object.class);

		// when
		service.get();

		// then
	}

	@Test
	public void GivenProvider_WhenToString_ThenContainsSERVICE() {
		// given

		// when
		String string = service.toString();

		// then
		assertThat(string, containsString("SERVICE"));
	}

	// --------------------------------------------------------------------------------------------

}
