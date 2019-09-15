package js.servlet.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Iterator;

import javax.servlet.http.Cookie;

import js.servlet.Cookies;
import js.unit.CookieStub;
import js.unit.HttpServletRequestStub;
import js.unit.HttpServletResponseStub;
import js.util.Classes;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CookiesUnitTest {
	private MockHttpServletRequest httpRequest;
	private MockHttpServletResponse httpResponse;

	@Before
	public void beforeTest() {
		httpRequest = new MockHttpServletRequest();
		httpResponse = new MockHttpServletResponse();
	}

	@After
	public void afterTest() {
		httpRequest.cookies = null;
	}

	@Test
	public void constructor() {
		httpRequest.cookies = new Cookie[] { new MockCookie("user", "John Doe") };
		Cookies cookies = new Cookies(httpRequest, httpResponse);

		assertNotNull(Classes.getFieldValue(cookies, "httpResponse"));
		assertNotNull(Classes.getFieldValue(cookies, "cookies"));
	}

	@Test
	public void get() {
		httpRequest.cookies = new Cookie[] { new MockCookie("user", "John Doe"), new MockCookie("password", "secret") };
		Cookies cookies = new Cookies(httpRequest, httpResponse);
		assertEquals("John Doe", cookies.get("user"));
	}

	@Test
	public void get_MissingCookie() {
		httpRequest.cookies = new Cookie[] { new MockCookie("user", "John Doe") };
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
		httpRequest.cookies = new Cookie[] { new MockCookie("user", "John Doe") };
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

		assertEquals("user", httpResponse.cookie.getName());
		assertEquals("John Doe", httpResponse.cookie.getValue());
		assertEquals("/", httpResponse.cookie.getPath());
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

		assertEquals("file", httpResponse.cookie.getName());
		assertEquals("/file/path", httpResponse.cookie.getValue());
		assertEquals("/", httpResponse.cookie.getPath());
	}

	@Test
	public void remove() {
		httpRequest.cookies = new Cookie[] { new MockCookie("user", "John Doe") };
		Cookies cookies = new Cookies(httpRequest, httpResponse);
		cookies.remove("user");

		assertEquals("user", httpResponse.cookie.getName());
		assertEquals("", httpResponse.cookie.getValue());
		assertEquals(0, httpResponse.cookie.getMaxAge());
		assertEquals("/", httpResponse.cookie.getPath());
	}

	@Test
	public void remove_MissingCookie() {
		httpRequest.cookies = new Cookie[] { new MockCookie("user", "John Doe") };
		Cookies cookies = new Cookies(httpRequest, httpResponse);
		cookies.remove("file");
		assertNull(httpResponse.cookie);
	}

	@Test
	public void remove_NoCookies() {
		Cookies cookies = new Cookies(httpRequest, httpResponse);
		cookies.remove("user");
	}

	@Test(expected = IllegalArgumentException.class)
	public void remove_NullName() {
		Cookies cookies = new Cookies(httpRequest, httpResponse);
		cookies.remove(null);
	}

	@Test
	public void iterator() {
		httpRequest.cookies = new Cookie[] { new MockCookie("user", "John Doe") };
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

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class MockHttpServletRequest extends HttpServletRequestStub {
		private Cookie[] cookies;

		@Override
		public Cookie[] getCookies() {
			return cookies;
		}
	}

	private static class MockHttpServletResponse extends HttpServletResponseStub {
		private Cookie cookie;

		@Override
		public void addCookie(Cookie cookie) {
			this.cookie = cookie;
		}
	}

	private static class MockCookie extends CookieStub {
		private static final long serialVersionUID = 4583348855673053411L;

		private String name;
		private String value;
		private String path;
		private int maxAge;

		public MockCookie(String name, String value) {
			super(name, value);
			this.name = name;
			this.value = value;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void setValue(String value) {
			this.value = value;
		}

		@Override
		public String getValue() {
			return value;
		}

		@Override
		public void setPath(String path) {
			this.path = path;
		}

		@Override
		public String getPath() {
			return path;
		}

		@Override
		public void setMaxAge(int maxAge) {
			this.maxAge = maxAge;
		}

		@Override
		public int getMaxAge() {
			return maxAge;
		}
	}
}
