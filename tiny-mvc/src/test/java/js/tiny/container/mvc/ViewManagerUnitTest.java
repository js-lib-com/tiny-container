package js.tiny.container.mvc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Locale;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.BugError;
import js.lang.ConfigBuilder;
import js.lang.ConfigException;
import js.lang.Configurable;
import js.tiny.container.servlet.RequestContext;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IFactory;
import js.tiny.container.unit.TestContext;
import js.util.Classes;
import js.util.Strings;

@SuppressWarnings({ "rawtypes", "unchecked" })
@RunWith(MockitoJUnitRunner.class)
public class ViewManagerUnitTest {
	@Mock
	private IContainer container;
	@Mock
	private RequestContext requestContext;

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("catalina.base", "fixture/server/tomcat");
	}

	@Before
	public void beforeTest() {
		when(container.getInstance(RequestContext.class)).thenReturn(requestContext);
	}

	@Test
	public void testViewMeta() throws Exception {
		TestContext.start();

		File file = new File("fixture/tomcat/webapps/app/about.htm");
		Class implementation = XspView.class;
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
				"	<repository path='src/test/resources/' files-pattern='*.html' />" + //
				"</views>";
		ConfigBuilder builder = new ConfigBuilder(config);
		Configurable viewManager = new ViewManagerImpl(container);
		viewManager.config(builder.build());
	}

	@Test
	public void config_MultipleRepositories() throws Exception {
		String config = "" + //
				"<views>" + //
				"	<repository path='src/test/resources/' files-pattern='*.html' />" + //
				"	<repository path='src/test/resources/' files-pattern='*.fo' />" + //
				"</views>";
		ConfigBuilder builder = new ConfigBuilder(config);
		Configurable viewManager = new ViewManagerImpl(container);
		viewManager.config(builder.build());
	}

	@Test
	public void getView() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);

		when(request.getRequestURI()).thenReturn("/test-app/page.htm");
		when(request.getContextPath()).thenReturn("/test-app");
		when(request.getLocale()).thenReturn(Locale.US);

		IFactory factory = TestContext.start();
		RequestContext context = factory.getInstance(RequestContext.class);
		context.attach(request, response);

		String config = "" + //
				"<views>" + //
				"	<repository path='src/test/resources/' files-pattern='*.html' />" + //
				"</views>";
		ConfigBuilder builder = new ConfigBuilder(config);
		ViewManagerImpl viewManager = new ViewManagerImpl(container);
		viewManager.config(builder.build());

		View view = viewManager.getView("page");
		assertNotNull(view);
		assertEquals("js.tiny.container.mvc.XspView", view.getClass().getName());
	}

	@Test(expected = BugError.class)
	public void getView_BadName() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);

		when(request.getRequestURI()).thenReturn("/test-app/page.htm");
		when(request.getContextPath()).thenReturn("/test-app");
		when(request.getLocale()).thenReturn(Locale.US);

		IFactory factory = TestContext.start();
		RequestContext context = factory.getInstance(RequestContext.class);
		context.attach(request, response);

		String config = "" + //
				"<views>" + //
				"	<repository path='src/test/resources/' files-pattern='*.html' />" + //
				"</views>";
		ConfigBuilder builder = new ConfigBuilder(config);
		ViewManagerImpl viewManager = new ViewManagerImpl(container);
		viewManager.config(builder.build());

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

	private void configException(String repository) throws Exception {
		String config = Strings.concat("<views>", repository, "</views>");
		ConfigBuilder builder = new ConfigBuilder(config);
		Configurable viewManager = new ViewManagerImpl(container);
		viewManager.config(builder.build());
	}
}
