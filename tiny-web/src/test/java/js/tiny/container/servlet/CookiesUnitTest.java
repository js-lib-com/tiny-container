package js.tiny.container.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Iterator;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.util.Classes;

@RunWith(MockitoJUnitRunner.class)
public class CookiesUnitTest {
	@Mock
	private HttpServletRequest httpRequest;
	@Mock
	private HttpServletResponse httpResponse;
	@Mock
	private Cookie cookie;

	@Before
	public void beforeTest() {
		when(cookie.getName()).thenReturn("user");
		when(cookie.getValue()).thenReturn("John Doe");
	}

	@Test
	public void constructor() {
		when(httpRequest.getCookies()).thenReturn(new Cookie[] { cookie });
		Cookies cookies = new Cookies(httpRequest, httpResponse);

		assertNotNull(Classes.getFieldValue(cookies, "httpResponse"));
		assertNotNull(Classes.getFieldValue(cookies, "cookies"));
	}

	@Test
	public void get() {
		when(httpRequest.getCookies()).thenReturn(new Cookie[] { cookie, mock(Cookie.class) });
		
		Cookies cookies = new Cookies(httpRequest, httpResponse);
		assertEquals("John Doe", cookies.get("user"));
	}

	@Test
	public void get_MissingCookie() {
		when(httpRequest.getCookies()).thenReturn(new Cookie[] { cookie });
		Cookies cookies = new Cookies(httpRequest, httpResponse);
		assertNull(cookies.get("password"));
	}

	@Test
	public void get_NoCookies() {
		Cookies cookies = new Cookies(httpRequest, httpResponse);
		assertNull(cookies.get("user"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void get_NullName() {
		Cookies cookies = new Cookies(httpRequest, httpResponse);
		cookies.get(null);
	}

	@Test
	public void has_Positive() {
		when(httpRequest.getCookies()).thenReturn(new Cookie[] { cookie });
		Cookies cookies = new Cookies(httpRequest, httpResponse);
		assertTrue(cookies.has("user"));
	}

	@Test
	public void has_Negative() {
		Cookies cookies = new Cookies(httpRequest, httpResponse);
		assertFalse(cookies.has("user"));
	}

	@Test
	public void add() {
		Cookies cookies = new Cookies(httpRequest, httpResponse);
		cookies.add("user", "John Doe");

		ArgumentCaptor<Cookie> cookieArgument = ArgumentCaptor.forClass(Cookie.class);
		verify(httpResponse, times(1)).addCookie(cookieArgument.capture());

		Cookie cookie = cookieArgument.getValue();
		assertEquals("user", cookie.getName());
		assertEquals("John Doe", cookie.getValue());
		assertEquals("/", cookie.getPath());
	}

	@Test(expected = IllegalArgumentException.class)
	public void add_NullName() {
		Cookies cookies = new Cookies(httpRequest, httpResponse);
		cookies.add(null, "John Doe");
	}

	@Test(expected = IllegalArgumentException.class)
	public void add_NullValue() {
		Cookies cookies = new Cookies(httpRequest, httpResponse);
		cookies.add("user", null);
	}

	@Test
	public void add_Object() {
		Cookies cookies = new Cookies(httpRequest, httpResponse);
		cookies.add("file", new File("/file/path"));

		ArgumentCaptor<Cookie> cookieArgument = ArgumentCaptor.forClass(Cookie.class);
		verify(httpResponse, times(1)).addCookie(cookieArgument.capture());

		Cookie cookie = cookieArgument.getValue();
		assertEquals("file", cookie.getName());
		assertEquals("/file/path", cookie.getValue());
		assertEquals("/", cookie.getPath());
	}

	@Test
	public void remove() {
		when(httpRequest.getCookies()).thenReturn(new Cookie[] { cookie });
		Cookies cookies = new Cookies(httpRequest, httpResponse);
		cookies.remove("user");

		verify(httpResponse, times(1)).addCookie(cookie);
		verify(cookie, times(1)).setMaxAge(0);
		verify(cookie, times(1)).setValue("");
		verify(cookie, times(1)).setPath("/");
	}

	@Test
	public void remove_MissingCookie() {
		when(httpRequest.getCookies()).thenReturn(new Cookie[] { cookie });
		Cookies cookies = new Cookies(httpRequest, httpResponse);
		cookies.remove("file");

		verify(httpResponse, times(0)).addCookie(any());
	}

	@Test
	public void remove_NoCookies() {
		Cookies cookies = new Cookies(httpRequest, httpResponse);
		cookies.remove("user");
		verify(httpResponse, times(0)).addCookie(any());
	}

	@Test(expected = IllegalArgumentException.class)
	public void remove_NullName() {
		Cookies cookies = new Cookies(httpRequest, httpResponse);
		cookies.remove(null);
	}

	@Test
	public void iterator() {
		when(httpRequest.getCookies()).thenReturn(new Cookie[] { cookie });
		Cookies cookies = new Cookies(httpRequest, httpResponse);
		Iterator<Cookie> iterator = cookies.iterator();

		assertTrue(iterator.hasNext());
		Cookie cookie = iterator.next();
		assertNotNull(cookie);
		assertEquals("user", cookie.getName());
		assertEquals("John Doe", cookie.getValue());
	}

	@Test
	public void iterator_NoCookies() {
		Cookies cookies = new Cookies(httpRequest, httpResponse);
		assertFalse(cookies.iterator().hasNext());
	}
}
