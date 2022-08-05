package com.jslib.container.mvc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.container.servlet.RequestContext;
import com.jslib.container.spi.IContainer;
import com.jslib.lang.BugError;
import com.jslib.lang.ConfigBuilder;
import com.jslib.lang.ConfigException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RunWith(MockitoJUnitRunner.class)
public class ViewManagerImplTest {
	@Mock
	private HttpServletRequest httpRequest;
	@Mock
	private HttpServletResponse httpResponse;

	@Mock
	private IContainer container;
	@Mock
	private RequestContext requestContext;

	private ViewManagerImpl viewManager;

	@Before
	public void beforeTest() {
		when(container.getInstance(RequestContext.class)).thenReturn(requestContext);
		requestContext.attach(httpRequest, httpResponse);

		viewManager = new ViewManagerImpl(container);
	}

	@Test
	public void GivenSingleReposiroey_WhenConfig_Then() throws Exception {
		// given
		String config = "" + //
				"<views>" + //
				"	<repository path='src/test/resources/' files-pattern='*.html' />" + //
				"</views>";

		// when
		viewManager.config(new ConfigBuilder(config).build());

		// then
	}

	@Test
	public void GivenMultipleRepositories_WhenConfig_Then() throws Exception {
		// given
		String config = "" + //
				"<views>" + //
				"	<repository path='src/test/resources/' files-pattern='*.html' />" + //
				"	<repository path='src/test/resources/' files-pattern='*.fo' />" + //
				"</views>";

		// when
		viewManager.config(new ConfigBuilder(config).build());

		// then
	}

	@Test
	public void GivenValidViewName_WhenGetView_ThenViewFound() throws Exception {
		// given
		String config = "" + //
				"<views>" + //
				"	<repository path='src/test/resources/' files-pattern='*.html' />" + //
				"</views>";
		viewManager.config(new ConfigBuilder(config).build());

		// when
		View view = viewManager.getView("page");

		// then
		assertNotNull(view);
		assertEquals("com.jslib.container.mvc.XspView", view.getClass().getName());
	}

	@Test(expected = BugError.class)
	public void GivenNotExistingViewName_WhenGetView_ThenException() throws Exception {
		// given
		String config = "" + //
				"<views>" + //
				"	<repository path='src/test/resources/' files-pattern='*.html' />" + //
				"</views>";
		viewManager.config(new ConfigBuilder(config).build());

		// when
		viewManager.getView("fake-page");

		// then
	}

	@Test(expected = ConfigException.class)
	public void GivenClassNotDound_WhenConfig_ThenException() throws Exception {
		// given
		String config = "" + //
				"<views>" + //
				"	<repository class='fake.Class' />" + //
				"</views>";

		// when
		viewManager.config(new ConfigBuilder(config).build());

		// then
	}

	@Test(expected = ConfigException.class)
	public void GivenClassNotView_WhenConfig_ThenException() throws Exception {
		// given
		String config = "" + //
				"<views>" + //
				"	<repository class='java.lang.Object' />" + //
				"</views>";

		// when
		viewManager.config(new ConfigBuilder(config).build());

		// then
	}

	@Test(expected = ConfigException.class)
	public void GivenAbstractView_WhenConfig_ThenException() throws Exception {
		// given
		String config = "" + //
				"<views>" + //
				"	<repository class='js.tiny.container.mvc.AbstractView' />" + //
				"</views>";

		// when
		viewManager.config(new ConfigBuilder(config).build());

		// then
	}

	@Test(expected = ConfigException.class)
	public void GivenMissingPathAttribute_WhenConfig_ThenException() throws Exception {
		// given
		String config = "" + //
				"<views>" + //
				"	<repository />" + //
				"</views>";

		// when
		viewManager.config(new ConfigBuilder(config).build());

		// then
	}

	@Test(expected = ConfigException.class)
	public void GivenMissingFilesPatternAttribute_WhenConfig_ThenException() throws Exception {
		// given
		String config = "" + //
				"<views>" + //
				"	<repository path='fixture/mvc' />" + //
				"</views>";

		// when
		viewManager.config(new ConfigBuilder(config).build());

		// then
	}
}
