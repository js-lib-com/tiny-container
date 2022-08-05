package com.jslib.container.mvc;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import jakarta.servlet.http.HttpServletResponse;

@RunWith(MockitoJUnitRunner.class)
public class RedirectTest {
	@Mock
	private HttpServletResponse httpResponse;
	
	@Test(expected = IllegalArgumentException.class)
	public void GivenNullArgument_WhenConstructor_ThenException() {
		new Redirect(null);
	}

	/** Empty string redirect should be accepted. */
	@Test
	public void GivenEmptyLocation_WhenSerialize_ThenSendRedirect() throws IOException {
		// given
		Redirect redirect = new Redirect("");
		
		// when
		redirect.serialize(httpResponse);
		
		// then
		verify(httpResponse, times(1)).sendRedirect("");
	}

	@Test
	public void GivenRelativePath_WhenSerialize_ThenSendRedirect() throws Exception {
		// given
		Redirect redirect = new Redirect("resource");
		
		// when
		redirect.serialize(httpResponse);
		
		// then
		verify(httpResponse, times(1)).sendRedirect("resource");
	}

	@Test
	public void GivenAbsolutePath_WhenSerialize_ThenSendRedirect() throws Exception {
		// given
		Redirect redirect = new Redirect("/resource");
		
		// when
		redirect.serialize(httpResponse);
		
		// then
		verify(httpResponse, times(1)).sendRedirect("/resource");
	}

	@Test
	public void GivenHostAbsolutePath_WhenSerialize_ThenSendRedirect() throws Exception {
		// given
		Redirect redirect = new Redirect("//server/resource");
		
		// when
		redirect.serialize(httpResponse);
		
		// then
		verify(httpResponse, times(1)).sendRedirect("//server/resource");
	}
}
