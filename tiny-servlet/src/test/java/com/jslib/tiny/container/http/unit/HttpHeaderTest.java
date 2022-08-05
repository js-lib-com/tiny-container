package com.jslib.tiny.container.http.unit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.tiny.container.http.HttpHeader;

import jakarta.servlet.http.HttpServletRequest;

@RunWith(MockitoJUnitRunner.class)
public class HttpHeaderTest {
	@Mock
	private HttpServletRequest request;

	@Before
	public void beforeTest() {
	}

	@Test
	public void constructor() {
		new HttpHeader() {
		};
	}

	@Test
	public void isXHR() {
		assertFalse(HttpHeader.isXHR(request));

		when(request.getHeader(HttpHeader.X_REQUESTED_WITH)).thenReturn("XMLHttpRequest");
		assertTrue(HttpHeader.isXHR(request));

		when(request.getHeader(HttpHeader.X_REQUESTED_WITH)).thenReturn("j(s)-lib android");
		assertFalse(HttpHeader.isXHR(request));
	}

	@Test
	public void isAndroid() {
		assertFalse(HttpHeader.isAndroid(request));

		when(request.getHeader(HttpHeader.X_REQUESTED_WITH)).thenReturn("j(s)-lib android");
		assertTrue(HttpHeader.isAndroid(request));

		when(request.getHeader(HttpHeader.X_REQUESTED_WITH)).thenReturn("XMLHttpRequest");
		assertFalse(HttpHeader.isAndroid(request));
	}

	@Test
	public void isSOAP() {
		assertFalse(HttpHeader.isSOAP(request));

		when(request.getHeader(HttpHeader.SOAP_ACTION)).thenReturn("action");
		assertTrue(HttpHeader.isSOAP(request));
	}
}
