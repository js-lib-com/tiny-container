package js.tiny.container.mvc.unit;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import js.tiny.container.mvc.Redirect;
import js.tiny.container.unit.HttpServletResponseStub;

public class RedirectUnitTest {
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
		MockHttpServletResponse httpResponse = new MockHttpServletResponse();
		Redirect redirect = new Redirect("resource");
		redirect.serialize(httpResponse);
		assertEquals("resource", httpResponse.location);
	}

	@Test
	public void serialize_Absolute() throws Exception {
		MockHttpServletResponse httpResponse = new MockHttpServletResponse();
		Redirect redirect = new Redirect("/resource");
		redirect.serialize(httpResponse);
		assertEquals("/resource", httpResponse.location);
	}

	@Test
	public void serialize_NetworkPath() throws Exception {
		MockHttpServletResponse httpResponse = new MockHttpServletResponse();
		Redirect redirect = new Redirect("//server/resource");
		redirect.serialize(httpResponse);
		assertEquals("//server/resource", httpResponse.location);
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE
	private static class MockHttpServletResponse extends HttpServletResponseStub {
		private String location;

		@Override
		public void sendRedirect(String location) throws IOException {
			this.location = location;
		}
	}
}
