package js.tiny.container.servlet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.BugError;
import js.tiny.container.core.InstanceKey;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.InstanceScope;

@RunWith(MockitoJUnitRunner.class)
public class SessionScopeFactoryTest {
	@Mock
	private IContainer container;
	@Mock
	private RequestContext requestContext;
	@Mock
	private HttpServletRequest httpRequest;
	@Mock
	private HttpSession httpSession;

	private SessionScopeFactory factory;

	@Before
	public void beforeTest() {
		when(container.getInstance(RequestContext.class)).thenReturn(requestContext);
		when(requestContext.getRequest()).thenReturn(httpRequest);
		when(httpRequest.getSession(true)).thenReturn(httpSession);

		factory = new SessionScopeFactory(container);
	}

	@Test
	public void GivenDefaults_WhenGetInstanceScope_ThenSESSION() {
		// given

		// when
		InstanceScope scope = factory.getInstanceScope();

		// then
		assertThat(scope, equalTo(InstanceScope.SESSION));
	}

	@Test
	public void GivenDefaults_WhenGetInstance_ThenGetSessionAttribute() {
		// given

		// when
		factory.getInstance(new InstanceKey("1"));

		// then
		verify(httpSession, times(1)).getAttribute("1");
	}

	@Test
	public void GivenDefaults_WhenPersistInstance_ThenSetSessionAttribute() {
		// given
		Object instance = new Object();

		// when
		factory.persistInstance(new InstanceKey("1"), instance);

		// then
		verify(httpSession, times(1)).setAttribute("1", instance);
	}

	@Test
	public void GivenDefaults_WhenClear_ThenDoesNothing() {
		// given

		// when
		factory.clear();

		// then
	}

	@Test
	public void GivenDefaults_WhenGetSession_ThenNotNull() throws Exception {
		// given

		// when
		HttpSession session = factory.getSession(new InstanceKey("1"));

		// then
		assertThat(session, notNullValue());
		verify(httpRequest, times(1)).getSession(true);
	}

	@Test(expected = BugError.class)
	public void GivenNullHttpRequest_WhenGetSession_ThenException() throws Exception {
		// given
		when(requestContext.getRequest()).thenReturn(null);

		// when
		factory.getSession(new InstanceKey("1"));

		// then
	}
}
