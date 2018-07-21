package js.http.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import js.http.HttpHeader;
import js.unit.HttpServletRequestStub;

import org.junit.Before;
import org.junit.Test;

public class HttpHeaderUnitTest {
	private MockHttpServletRequest httpRequest;

	@Before
	public void beforeTest() {
		httpRequest = new MockHttpServletRequest();
	}

	@Test
	public void constructor() {
		new HttpHeader() {
		};
	}

	@Test
	public void isXHR() {
		assertFalse(HttpHeader.isXHR(httpRequest));

		httpRequest.headers.put(HttpHeader.X_REQUESTED_WITH, "XMLHttpRequest");
		assertTrue(HttpHeader.isXHR(httpRequest));

		httpRequest.headers.put(HttpHeader.X_REQUESTED_WITH, "j(s)-lib android");
		assertFalse(HttpHeader.isXHR(httpRequest));
	}

	@Test
	public void isAndroid() {
		assertFalse(HttpHeader.isAndroid(httpRequest));

		httpRequest.headers.put(HttpHeader.X_REQUESTED_WITH, "j(s)-lib android");
		assertTrue(HttpHeader.isAndroid(httpRequest));

		httpRequest.headers.put(HttpHeader.X_REQUESTED_WITH, "XMLHttpRequest");
		assertFalse(HttpHeader.isAndroid(httpRequest));
	}

	@Test
	public void isSOAP() {
		assertFalse(HttpHeader.isSOAP(httpRequest));

		httpRequest.headers.put(HttpHeader.SOAP_ACTION, "action");
		assertTrue(HttpHeader.isSOAP(httpRequest));
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	@SuppressWarnings("unchecked")
	class MockHttpServletRequest extends HttpServletRequestStub {
		private Map<String, String> headers = new HashMap<>();

		@Override
		public String getHeader(String name) {
			return headers.get(name);
		}
	}
}
