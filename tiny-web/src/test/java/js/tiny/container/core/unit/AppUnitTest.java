package js.tiny.container.core.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;

import js.lang.Config;
import js.lang.ConfigBuilder;
import js.lang.ConfigException;
import js.lang.Configurable;
import js.tiny.container.servlet.App;
import js.tiny.container.servlet.AppContext;
import js.tiny.container.servlet.TinyContainer;
import js.tiny.container.stub.AppContextStub;

@SuppressWarnings("hiding")
public class AppUnitTest {
	@BeforeClass
	public static void beforeClass() {
		System.setProperty("catalina.base", "fixture/server/tomcat");
	}

	@Test
	public void constructor() throws ConfigException {
		App app = new App(new MockAppContext());
		assertNotNull(app.getContext());
	}

	@Test
	public void postConstruct() throws Exception {
		class MockApp extends App {
			private int onCreateProbe;

			public MockApp(AppContext context) {
				super(context);
			}

			@Override
			protected void onCreate(AppContext context) throws Exception {
				++onCreateProbe;
				assertEquals(MockAppContext.class, context.getClass());
			}
		}

		MockApp app = new MockApp(new MockAppContext());
		app.postConstruct();
		assertEquals(1, app.onCreateProbe);
	}

	@Test
	public void preDestroy() throws Exception {
		class MockApp extends App {
			private int onDestroyProbe;

			public MockApp(AppContext context) {
				super(context);
			}

			@Override
			protected void onDestroy(AppContext context) throws Exception {
				++onDestroyProbe;
				assertEquals(MockAppContext.class, context.getClass());
			}
		}

		MockApp app = new MockApp(new MockAppContext());
		app.preDestroy();
		assertEquals(1, app.onDestroyProbe);
	}

	@Test
	public void postConstruct_Container() throws ConfigException {
		String descriptor = "" + //
				"<descriptor>" + //
				"	<managed-classes>" + //
				"		<app interface='js.tiny.container.servlet.App' class='js.tiny.container.core.unit.AppUnitTest$MockApp' />" + //
				"		<app-context interface='js.tiny.container.servlet.AppContext' class='js.tiny.container.core.unit.AppUnitTest$MockAppContext'  />" + //
				"	</managed-classes>" + //
				"	<app>" + //
				"	</app>" + //
				"</descriptor>";
		ConfigBuilder builder = new ConfigBuilder(descriptor);

		TinyContainer container = new TinyContainer();
		container.config(builder.build());

		MockApp app = container.getInstance(App.class);
		assertEquals(1, app.configProbe);
		assertEquals(1, app.postConstructProbe);
		assertEquals(0, app.preDestroyProbe);
	}

	@Test
	public void preDestroy_Container() throws ConfigException {
		String descriptor = "" + //
				"<descriptor>" + //
				"	<managed-classes>" + //
				"		<app interface='js.tiny.container.servlet.App' class='js.tiny.container.core.unit.AppUnitTest$MockApp' />" + //
				"		<app-context interface='js.tiny.container.servlet.AppContext' class='js.tiny.container.core.unit.AppUnitTest$MockAppContext'  />" + //
				"	</managed-classes>" + //
				"	<app>" + //
				"	</app>" + //
				"</descriptor>";
		ConfigBuilder builder = new ConfigBuilder(descriptor);

		TinyContainer container = new TinyContainer();
		container.config(builder.build());
		container.start();

		MockApp app = container.getInstance(App.class);
		container.destroy();

		assertEquals(1, app.configProbe);
		assertEquals(1, app.postConstructProbe);
		assertEquals(1, app.preDestroyProbe);
	}

	@Test
	public void subclassConfig() throws ConfigException {
		String descriptor = "" + //
				"<descriptor>" + //
				"	<managed-classes>" + //
				"		<app interface='js.tiny.container.servlet.App' class='js.tiny.container.core.unit.AppUnitTest$MockApp' />" + //
				"		<app-context interface='js.tiny.container.servlet.AppContext' class='js.tiny.container.core.unit.AppUnitTest$MockAppContext'  />" + //
				"	</managed-classes>" + //
				"	<app>" + //
				"	</app>" + //
				"</descriptor>";
		ConfigBuilder builder = new ConfigBuilder(descriptor);

		TinyContainer container = new TinyContainer();
		container.config(builder.build());

		MockApp app = container.getInstance(App.class);
		assertEquals(1, app.configProbe);
		assertEquals(1, app.postConstructProbe);
		assertEquals(0, app.preDestroyProbe);
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class MockAppContext extends AppContextStub {
		public MockAppContext() {
		}

		@Override
		public String getAppName() {
			return "test-app";
		}
	}

	private static class MockApp extends App implements Configurable {
		private int postConstructProbe;
		private int preDestroyProbe;
		private int configProbe;

		public MockApp(AppContext context) {
			super(context);
		}

		@Override
		public void postConstruct() throws Exception {
			++postConstructProbe;
		}

		@Override
		public void preDestroy() throws Exception {
			++preDestroyProbe;
		}

		@Override
		public void config(Config config) throws Exception {
			++configProbe;
		}
	}
}
