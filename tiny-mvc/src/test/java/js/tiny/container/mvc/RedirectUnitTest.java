package js.tiny.container.mvc;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RedirectUnitTest {
	@Mock
	private HttpServletResponse httpResponse;
	
	@Test(expected = IllegalArgumentException.class)
	public void constructor_NullLocation() {
		new Redirect(null);
	}

	/** Empty string redirect should be accepted. */
	@Test
	public void constructor_EmptyLocation() {
		new Redirect("");
	}

	@Test
	public void serialize_Relative() throws Exception {
		Redirect redirect = new Redirect("resource");
		redirect.serialize(httpResponse);
		
		verify(httpResponse, times(1)).sendRedirect("resource");
	}

	@Test
	public void serialize_Absolute() throws Exception {
		Redirect redirect = new Redirect("/resource");
		redirect.serialize(httpResponse);
		verify(httpResponse, times(1)).sendRedirect("/resource");
	}

	@Test
	public void serialize_NetworkPath() throws Exception {
		Redirect redirect = new Redirect("//server/resource");
		redirect.serialize(httpResponse);
		verify(httpResponse, times(1)).sendRedirect("//server/resource");
	}
}
