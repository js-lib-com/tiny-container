package js.tiny.container.servlet;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

import java.security.Principal;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.ConfigException;
import js.tiny.container.cdi.CDI;
import js.tiny.container.spi.ISecurityContext;

@RunWith(MockitoJUnitRunner.class)
public class TinyContainerSecurityTest {
	@Mock
	private ISecurityContext security;
	@Mock
	private CDI cdi;

	private TinyContainer container;

	@Before
	public void beforeTest() throws ConfigException {
		when(cdi.getInstance(ISecurityContext.class)).thenReturn(security);

		container = new TinyContainer(cdi);
		container.create(Collections.emptyList());
	}

	@Test
	public void GivenCredentials_WhenLoginUserAndPassword_ThenDelegate() {
		// given

		// when
		container.login("Tom Joad", "secret");
		
		// then
		verify(security, times(1)).login("Tom Joad", "secret");
	}

	@Test(expected = IllegalStateException.class)
	public void GivenMissingSecurityProvider_WhenLoginUserAndPassword_ThenException() {
		// given
		when(cdi.getInstance(ISecurityContext.class)).thenReturn(null);
		container.create(Collections.emptyList());

		// when
		container.login("Tom Joad", "secret");
		
		// then
	}

	@Test
	public void GivenPrincipal_WhenLoginPrincipal_ThenDelegate() {
		// given
		Principal principal = mock(Principal.class);

		// when
		container.login(principal);
		
		// then
		verify(security, times(1)).login(principal);
	}

	@Test(expected = IllegalStateException.class)
	public void GivenMissingSecurityProvider_WhenLoginPrincipal_ThenException() {
		// given
		when(cdi.getInstance(ISecurityContext.class)).thenReturn(null);
		container.create(Collections.emptyList());
		Principal principal = mock(Principal.class);

		// when
		container.login(principal);
		
		// then
	}

	@Test
	public void GivenDefaults_WhenLogout_ThenDelegate() {
		// given

		// when
		container.logout();
		
		// then
		verify(security, times(1)).logout();
	}

	@Test(expected = IllegalStateException.class)
	public void GivenMissingSecurityProvider_WhenLogout_ThenException() {
		// given
		when(cdi.getInstance(ISecurityContext.class)).thenReturn(null);
		container.create(Collections.emptyList());

		// when
		container.logout();
		
		// then
	}

	@Test
	public void GivenSecurityPrincipal_WhenGetUserPrincipal_ThenDelegate() {
		// given
		Principal principal = mock(Principal.class);
		when(security.getUserPrincipal()).thenReturn(principal);

		// when
		Principal userPrincipal = container.getUserPrincipal();
		
		// then
		assertThat(userPrincipal, notNullValue());
		assertThat(userPrincipal, equalTo(principal));
		verify(security, times(1)).getUserPrincipal();
	}

	@Test(expected = IllegalStateException.class)
	public void GivenMissingSecurityProvider_WhenGetUserPrincipal_ThenException() {
		// given
		when(cdi.getInstance(ISecurityContext.class)).thenReturn(null);
		container.create(Collections.emptyList());

		// when
		container.getUserPrincipal();
		
		// then
	}

	@Test
	public void GivenSecurityAutheticated_WhenIsAuthenticated_ThenDelegate() {
		// given
		when(security.isAuthenticated()).thenReturn(true);

		// when
		boolean authenticated = container.isAuthenticated();
		
		// then
		assertThat(authenticated, equalTo(true));
		verify(security, times(1)).isAuthenticated();
	}

	@Test(expected = IllegalStateException.class)
	public void GivenMissingSecurityProvider_WhenIsAuthenticated_ThenException() {
		// given
		when(cdi.getInstance(ISecurityContext.class)).thenReturn(null);
		container.create(Collections.emptyList());

		// when
		container.isAuthenticated();
		
		// then
	}

	@Test
	public void Given_When_Then() {
		// given

		// when

		// then
	}
}