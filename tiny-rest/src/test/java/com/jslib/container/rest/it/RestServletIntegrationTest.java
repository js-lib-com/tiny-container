package com.jslib.container.rest.it;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.container.rest.RestServlet;
import com.jslib.container.servlet.TinyContainer;
import com.jslib.lang.ConfigBuilder;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RunWith(MockitoJUnitRunner.class)
public class RestServletIntegrationTest {
	private static final String DESCRIPTOR = "" + //
			"<module package='com.jslib.container.rest.it'>" + //
			"	<binding bind='Service' in='jakarta.inject.Singleton' />" + //
			"</module>";

	@Mock
	private ServletContext servletContext;
	@Mock
	private ServletConfig servletConfig;
	@Mock
	private ServletRequestEvent requestEvent;
	@Mock
	private HttpServletRequest httpRequest;
	@Mock
	private HttpServletResponse httpResponse;

	private OutputStream outputStream = new OutputStream();
	private InputStream inputStream = new InputStream();

	private TinyContainer container;

	private RestServlet servlet;

	@Before
	public void beforeTest() throws Exception {
		container = new TinyContainer();
		container.configure(new ConfigBuilder(DESCRIPTOR).build());

		when(servletConfig.getServletContext()).thenReturn(servletContext);
		when(servletConfig.getServletName()).thenReturn("RestServlet");
		
		when(servletContext.getServletContextName()).thenReturn("app");
		when(servletContext.getAttribute(TinyContainer.ATTR_INSTANCE)).thenReturn(container);

		when(httpRequest.getMethod()).thenReturn("POST");
		when(httpRequest.getContextPath()).thenReturn("/test");
		when(httpRequest.getLocale()).thenReturn(Locale.getDefault());
		when(httpRequest.getInputStream()).thenReturn(inputStream);
		when(httpResponse.getOutputStream()).thenReturn(outputStream);
		when(httpRequest.getParameterNames()).thenReturn(Collections.emptyEnumeration());

		servlet = new RestServlet();
		servlet.init(servletConfig);

		when(requestEvent.getServletRequest()).thenReturn(httpRequest);
		container.requestInitialized(requestEvent);
	}

	@After
	public void afterTest() {
		container.requestDestroyed(requestEvent);
	}

	@Test
	public void GivenGETString_WhenService_Then200String() throws Exception {
		// given
		when(httpRequest.getMethod()).thenReturn("GET");
		when(httpRequest.getRequestURI()).thenReturn("/test/rest/name");
		when(httpRequest.getPathInfo()).thenReturn("/name");

		// when
		servlet.service(httpRequest, httpResponse);

		// then
		verify(httpResponse, times(1)).setStatus(200);
		verify(httpResponse, times(1)).setCharacterEncoding("UTF-8");

		assertThat(outputStream.toString(), equalTo("\"Jane Doe\""));
	}

	@Test
	public void GivenPOSTString_WhenService_Then204() throws Exception {
		// given
		when(httpRequest.getRequestURI()).thenReturn("/test/rest/name");
		when(httpRequest.getPathInfo()).thenReturn("/name");
		inputStream.content("\"John Doe\"");

		// when
		servlet.service(httpRequest, httpResponse);

		// then
		verify(httpResponse, times(1)).setStatus(204);
		verify(httpResponse, times(1)).setCharacterEncoding("UTF-8");

		assertThat(outputStream.toString(), is(emptyString()));
	}

	@Test
	public void GivenPOSTPathParam_WhenService_Then() throws Exception {
		// given
		when(httpRequest.getRequestURI()).thenReturn("/test/rest/call/0770555666");
		when(httpRequest.getPathInfo()).thenReturn("/call/0770555666");
		
		// when
		servlet.service(httpRequest, httpResponse);
		
		// then
		verify(httpResponse, times(1)).setStatus(204);
		verify(httpResponse, times(1)).setCharacterEncoding("UTF-8");

		assertThat(outputStream.toString(), is(emptyString()));
	}

	@Test
	public void Given_WhenService_Then() {
		// given
		
		// when
		
		// then
	}
	
	// --------------------------------------------------------------------------------------------

	private static class InputStream extends ServletInputStream {
		private StringReader stringReader;

		public void content(String content) {
			this.stringReader = new StringReader(content);
		}

		@Override
		public int read() throws IOException {
			return stringReader.read();
		}

		@Override
		public boolean isFinished() {
			return false;
		}

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public void setReadListener(ReadListener readListener) {
		}
	}

	private static class OutputStream extends ServletOutputStream {
		private StringWriter stringWriter;

		public OutputStream() {
			this.stringWriter = new StringWriter();
		}

		@Override
		public void write(int b) throws IOException {
			stringWriter.append((char) b);
		}

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
		}

		@Override
		public String toString() {
			return stringWriter.toString();
		}
	}
}
