package js.mvc.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Locale;
import java.util.Properties;

import js.core.AppFactory;
import js.lang.BugError;
import js.lang.ConfigBuilder;
import js.lang.ConfigException;
import js.lang.Configurable;
import js.mvc.View;
import js.mvc.ViewManager;
import js.mvc.ViewMeta;
import js.servlet.RequestContext;
import js.unit.HttpServletRequestStub;
import js.unit.HttpServletResponseStub;
import js.unit.TestContext;
import js.util.Classes;
import js.util.Strings;

import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class ViewManagerUnitTest {
	@BeforeClass
	public static void beforeClass() throws Exception {
	}

	@Test
	public void testViewMeta() throws Exception {
		TestContext.start();

		File file = new File("fixture/tomcat/webapps/app/about.htm");
		Class implementation = Classes.forName("js.mvc.XspView");
		Properties properties = new Properties();

		ViewMeta meta = new ViewMeta(file, implementation, properties);

		assertNotNull(meta.getName());
		assertEquals("about", meta.getName());
		assertNotNull(meta.getTemplateFile());
		assertEquals(file, meta.getTemplateFile());
		assertNotNull(meta.getTemplateFile());
		assertNotNull(meta.getImplementation());
		assertEquals(implementation, meta.getImplementation());
		assertNotNull(Classes.getFieldValue(meta, "properties"));
		assertEquals(properties, Classes.getFieldValue(meta, "properties"));
		assertFalse(meta.hasProperty("fake.property"));
	}

	@Test
	public void config() throws Exception {
		String config = "" + //
				"<views>" + //
				"	<repository path='fixture/mvc' files-pattern='*.html' />" + //
				"</views>";
		ConfigBuilder builder = new ConfigBuilder(config);
		Configurable viewManager = Classes.newInstance("js.mvc.ViewManagerImpl");
		viewManager.config(builder.build());
	}

	@Test
	public void config_MultipleRepositories() throws Exception {
		String config = "" + //
				"<views>" + //
				"	<repository path='fixture/mvc' files-pattern='*.html' />" + //
				"	<repository path='fixture/mvc' files-pattern='*.fo' />" + //
				"</views>";
		ConfigBuilder builder = new ConfigBuilder(config);
		Configurable viewManager = Classes.newInstance("js.mvc.ViewManagerImpl");
		viewManager.config(builder.build());
	}

	@Test
	public void getView() throws Exception {
		MockHttpServletRequest httpRequest = new MockHttpServletRequest();
		MockHttpServletResponse httpResponse = new MockHttpServletResponse();

		AppFactory factory = TestContext.start();
		RequestContext context = factory.getInstance(RequestContext.class);
		context.attach(httpRequest, httpResponse);

		String config = "" + //
				"<views>" + //
				"	<repository path='fixture/mvc' files-pattern='*.html' />" + //
				"</views>";
		ConfigBuilder builder = new ConfigBuilder(config);
		ViewManager viewManager = Classes.newInstance("js.mvc.ViewManagerImpl");
		Classes.invoke(viewManager, "config", builder.build());

		View view = viewManager.getView("page");
		assertNotNull(view);
		assertEquals("js.mvc.XspView", view.getClass().getName());
	}

	@Test(expected = BugError.class)
	public void getView_BadName() throws Exception {
		MockHttpServletRequest httpRequest = new MockHttpServletRequest();
		MockHttpServletResponse httpResponse = new MockHttpServletResponse();

		AppFactory factory = TestContext.start();
		RequestContext context = factory.getInstance(RequestContext.class);
		context.attach(httpRequest, httpResponse);

		String config = "" + //
				"<views>" + //
				"	<repository path='fixture/mvc' files-pattern='*.html' />" + //
				"</views>";
		ConfigBuilder builder = new ConfigBuilder(config);
		ViewManager viewManager = Classes.newInstance("js.mvc.ViewManagerImpl");
		Classes.invoke(viewManager, "config", builder.build());

		viewManager.getView("fake-page");
	}

	@Test(expected = ConfigException.class)
	public void config_BadClass() throws Exception {
		configException("<repository class='fake.Class' />");
	}

	@Test(expected = ConfigException.class)
	public void config_NoViewClass() throws Exception {
		configException("<repository class='java.lang.Object' />");
	}

	@Test(expected = ConfigException.class)
	public void config_AbstractView() throws Exception {
		configException("<repository class='js.mvc.AbstractView' />");
	}

	@Test(expected = ConfigException.class)
	public void config_NoPath() throws Exception {
		configException("<repository />");
	}

	@Test(expected = ConfigException.class)
	public void config_NoFilesPattern() throws Exception {
		configException("<repository path='fixture/mvc' />");
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static void configException(String repository) throws Exception {
		String config = Strings.concat("<views>", repository, "</views>");
		ConfigBuilder builder = new ConfigBuilder(config);
		Configurable viewManager = Classes.newInstance("js.mvc.ViewManagerImpl");
		viewManager.config(builder.build());
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class MockHttpServletRequest extends HttpServletRequestStub {
		@Override
		public String getRequestURI() {
			return "/test-app/page.htm";
		}

		@Override
		public String getContextPath() {
			return "/test-app";
		}

		@Override
		public Locale getLocale() {
			return Locale.US;
		}
	}

	private static class MockHttpServletResponse extends HttpServletResponseStub {
	}
}
