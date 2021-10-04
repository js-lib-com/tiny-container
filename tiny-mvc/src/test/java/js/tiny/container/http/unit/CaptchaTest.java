package js.tiny.container.http.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.BugError;
import js.lang.ConfigBuilder;
import js.lang.ConfigException;
import js.lang.Configurable;
import js.tiny.container.core.AppContext;
import js.tiny.container.http.NoSuchResourceException;
import js.tiny.container.http.captcha.Challenge;
import js.tiny.container.servlet.RequestContext;
import js.util.Classes;
import js.util.Files;
import js.util.Strings;

@RunWith(MockitoJUnitRunner.class)
public class CaptchaTest {
	private static final String CAPTCHA = "js.tiny.container.http.captcha.Captcha";
	private static final String CHALLENGE = "js.tiny.container.http.captcha.Challenge";

	@Mock
	private AppContext context;

	@Mock
	private RequestContext request;

	@Mock
	private HttpSession session;

	/** Config method should initialize captcha manager state from configuration object. */
	@Test
	public void captcha_config() throws Exception {
		String REPOSITORY_DIR = new File("src/test/resources/captcha").getAbsolutePath();
		Object captcha = config(REPOSITORY_DIR, 5);
		assertNotNull(captcha);
		assertEquals(REPOSITORY_DIR, ((File) Classes.getFieldValue(captcha, "imagesRepositoryDir")).getPath());
		assertEquals(5, (int)Classes.getFieldValue(captcha, "challengeSetSize"));
	}

	@Test(expected = ConfigException.class)
	public void captcha_config_NullRepository() throws Exception {
		String config = "" + //
				"<captcha>" + //
				"	<property name='captcha.repository.dir' value='fixture/captcha' />" + //
				"</captcha>";
		captcha(config, null);
	}

	@Test(expected = ConfigException.class)
	public void captcha_config_MissingRepository() throws Exception {
		config("fake-captcha");
	}

	@Test(expected = ConfigException.class)
	public void captcha_config_RepositoryNotDriectory() throws Exception {
		config("src/test/resources/captcha/apple.png");
	}

	@Test(expected = ConfigException.class)
	public void captcha_config_RepositoryNotAbsolute() throws Exception {
		config("src/test/resources/captcha");
	}

	@Test(expected = ConfigException.class)
	public void captcha_config_EmptyRepository() throws Exception {
		config(new File("src/test/resources/captcha/empty"));
	}

	@Test(expected = ConfigException.class)
	public void captcha_config_SetSizeTooLarge() throws Exception {
		config(new File("src/test/resources/captcha"), 10);
	}

	/** CAPTCHA challenge getter should create a new challenge and store on HTTP session challenges store. */
	@Test
	public void captcha_getChallenge() throws Exception {
		when(context.getInstance(RequestContext.class)).thenReturn(request);
		when(request.getSession(true)).thenReturn(session);
		when(session.getAttribute("challenges-key")).thenReturn(new HashMap<Integer, Challenge>());

		Object captcha = config(new File("src/test/resources/captcha"), 5, context);
		Object challenge = Classes.invoke(captcha, "getChallenge", 0);
		assertNotNull(challenge);

		// assert challenges store from HTTP session contains the newly created challenge
		Map<Integer, Object> challenges = Classes.invoke(captcha, "getChallenges");
		assertEquals(challenge, challenges.get(0));
	}

	@Test(expected = BugError.class)
	public void captcha_getChallenge_NullRepository() throws Exception {
		Configurable captcha = Classes.newInstance(CAPTCHA, (Object) null);
		Classes.invoke(captcha, "getChallenge", 0);
	}

	/**
	 * CAPTCHA challenge image is retrieved after challenge creation. Test that all tokens from challenges set have related
	 * image.
	 */
	@Test
	public void captcha_getImage() throws Exception {
		when(context.getInstance(RequestContext.class)).thenReturn(request);
		when(request.getSession(true)).thenReturn(session);
		when(session.getAttribute("challenges-key")).thenReturn(new HashMap<Integer, Challenge>());

		Object captcha = config(new File("src/test/resources/captcha"), 5, context);
		Object challenge = Classes.invoke(captcha, "getChallenge", 0);
		assertNotNull(challenge);

		Map<String, File> tokenedImageFiles = Classes.getFieldValue(challenge, "tokenedImageFiles");
		assertNotNull(tokenedImageFiles);
		for (String token : tokenedImageFiles.keySet()) {
			assertNotNull(Classes.invoke(captcha, "getImage", token));
		}
	}

	/** Get challenge image test with a large number of CAPTCHA instances. */
	@Test
	public void captcha_getImage_MultipleCaptchaInstances() throws Exception {
		when(context.getInstance(RequestContext.class)).thenReturn(request);
		when(request.getSession(true)).thenReturn(session);
		when(session.getAttribute("challenges-key")).thenReturn(new HashMap<Integer, Challenge>());

		Object captcha = config(new File("src/test/resources/captcha"), 5, context);

		List<String> tokens = new ArrayList<>();
		for (int i = 0; i < 100; ++i) {
			Object challenge = Classes.invoke(captcha, "getChallenge", i);
			assertNotNull(challenge);

			Map<String, File> tokenedImageFiles = Classes.getFieldValue(challenge, "tokenedImageFiles");
			assertNotNull(tokenedImageFiles);
			for (String token : tokenedImageFiles.keySet()) {
				tokens.add(token);
			}
		}

		for (String token : tokens) {
			assertNotNull(Classes.invoke(captcha, "getImage", token));
		}
	}

	/** Missing challenge from session is considered resource not found. */
	@Test(expected = NoSuchResourceException.class)
	public void captcha_getImage_NoChallenge() throws Exception {
		when(context.getInstance(RequestContext.class)).thenReturn(request);
		when(request.getSession(true)).thenReturn(session);

		Object captcha = config(new File("src/test/resources/captcha"), 5, context);
		Classes.invoke(captcha, "getImage", Strings.UUID());
	}

	@Test(expected = NoSuchResourceException.class)
	public void captcha_getImage_BadToken() throws Exception {
		when(context.getInstance(RequestContext.class)).thenReturn(request);
		when(request.getSession(true)).thenReturn(session);

		Object captcha = config(new File("src/test/resources/captcha"), 1, context);
		// getChallenge(int) should be called in order to generate challenge internal images set
		Classes.invoke(captcha, "getChallenge", 0);
		Classes.invoke(captcha, "getImage", Strings.UUID());
	}

	@Test
	public void captcha_verifyResponse() throws Exception {
		int instanceIndex = 0;

		when(context.getInstance(RequestContext.class)).thenReturn(request);
		when(request.getSession(true)).thenReturn(session);
		when(session.getAttribute("challenges-key")).thenReturn(new HashMap<Integer, Challenge>());

		Object captcha = config(new File("src/test/resources/captcha"), 5, context);
		Object challenge = Classes.invoke(captcha, "getChallenge", instanceIndex);
		assertNotNull(challenge);

		String value = Classes.getFieldValue(challenge, "value");
		assertNotNull(value);
		Map<String, File> tokenedImageFiles = Classes.getFieldValue(challenge, "tokenedImageFiles");
		assertNotNull(tokenedImageFiles);

		for (Map.Entry<String, File> entry : tokenedImageFiles.entrySet()) {
			if (Files.basename(entry.getValue()).equals(value)) {
				assertNull(Classes.invoke(captcha, "verifyResponse", instanceIndex, entry.getKey()));
				break;
			}
		}
		assertNotNull(Classes.invoke(captcha, "verifyResponse", instanceIndex, Strings.UUID()));
	}

	@Test
	public void captcha_verifyResponse_MultipleCaptchaInstances() throws Exception {
		when(context.getInstance(RequestContext.class)).thenReturn(request);
		when(request.getSession(true)).thenReturn(session);
		when(session.getAttribute("challenges-key")).thenReturn(new HashMap<Integer, Challenge>());

		Object captcha = config(new File("src/test/resources/captcha"), 5, context);

		for (int i = 0; i < 100; ++i) {
			Object challenge = Classes.invoke(captcha, "getChallenge", i);
			assertNotNull(challenge);

			String value = Classes.getFieldValue(challenge, "value");
			assertNotNull(value);
			Map<String, File> tokenedImageFiles = Classes.getFieldValue(challenge, "tokenedImageFiles");
			assertNotNull(tokenedImageFiles);

			for (Map.Entry<String, File> entry : tokenedImageFiles.entrySet()) {
				if (Files.basename(entry.getValue()).equals(value)) {
					assertNull(Classes.invoke(captcha, "verifyResponse", i, entry.getKey()));
					break;
				}
			}
			assertNotNull(Classes.invoke(captcha, "verifyResponse", i, Strings.UUID()));
		}
	}

	/** Invoking challenge response verify with no challenge on session should rise illegal state. */
	@Test(expected = IllegalStateException.class)
	public void captcha_verifyResponse_NoChallenge() throws Exception {
		when(context.getInstance(RequestContext.class)).thenReturn(request);
		when(request.getSession(true)).thenReturn(session);

		Object captcha = config(new File("src/test/resources/captcha"), 5, context);
		Classes.invoke(captcha, "verifyResponse", 0, Strings.UUID());
	}

	/** Empty token on challenge response verify should rise illegal argument. */
	@Test(expected = IllegalArgumentException.class)
	public void captcha_verifyResponse_EmptyToken() throws Exception {
		Classes.invoke(Classes.newInstance(CAPTCHA, (Object) null), "verifyResponse", 0, "");
	}

	/** Null token on challenge response verify should rise illegal argument. */
	@Test(expected = IllegalArgumentException.class)
	public void captcha_verifyResponse_NullToken() throws Exception {
		Classes.invoke(Classes.newInstance(CAPTCHA, (Object) null), "verifyResponse", 0, null);
	}

	/**
	 * Challenge contains two fields visible on client script by name. These names should not be changed and this test ensure
	 * that. Also challenge images name is a request to a dynamic resource with format <code>captcha/image?token</code>.
	 */
	@Test
	public void challenge_Constructor() {
		File repository = new File("src/test/resources/captcha");
		int setSize = 3;
		Object challenge = Classes.newInstance(CHALLENGE, repository, setSize);
		assertNotNull(challenge);

		List<String> images = Classes.getFieldValue(challenge, "images");
		assertNotNull(images);
		assertEquals(setSize, images.size());
		for (String image : images) {
			assertTrue(image.startsWith("captcha/image?"));
		}

		String value = Classes.getFieldValue(challenge, "value");
		assertNotNull(value);

		boolean valueFound = false;
		for (File file : repository.listFiles()) {
			if (value.equals(Files.basename(file))) {
				valueFound = true;
				break;
			}
		}
		assertTrue(valueFound);
	}

	@Test
	public void challenge_captchaImage() throws Exception {
		File repository = new File("src/test/resources/captcha");
		Object challenge = Classes.newInstance(CHALLENGE, repository, 3);

		Map<String, File> tokenedImageFiles = Classes.getFieldValue(challenge, "tokenedImageFiles");
		assertNotNull(tokenedImageFiles);
		String token = tokenedImageFiles.keySet().iterator().next();
		assertEquals(tokenedImageFiles.get(token), (File) Classes.invoke(challenge, "getImage", token));

		assertNull(Classes.invoke(challenge, "getImage", (String) null));
		assertNull(Classes.invoke(challenge, "getImage", ""));
	}

	@Test
	public void challenge_verifyResponse() throws Exception {
		File repository = new File("src/test/resources/captcha");
		Object challenge = Classes.newInstance(CHALLENGE, repository, 1);

		Map<String, File> tokenedImageFiles = Classes.getFieldValue(challenge, "tokenedImageFiles");
		assertNotNull(tokenedImageFiles);
		String token = tokenedImageFiles.keySet().iterator().next();

		assertTrue((boolean) Classes.invoke(challenge, "verifyResponse", token));
		assertFalse((boolean) Classes.invoke(challenge, "verifyResponse", "fake-token"));
		assertFalse((boolean) Classes.invoke(challenge, "verifyResponse", ""));
		assertFalse((boolean) Classes.invoke(challenge, "verifyResponse", (String) null));
	}

	@Test
	public void challenge_getValue() throws Exception {
		Class<?> challengeClass = Classes.forName(CHALLENGE);
		assertEquals("file name", Classes.invoke(challengeClass, "getValue", new File("path/file-name.png")));
		assertEquals("file name", Classes.invoke(challengeClass, "getValue", new File("path/file.name.png")));
		assertEquals("file name", Classes.invoke(challengeClass, "getValue", new File("path/file_name.png")));
		assertEquals("file name", Classes.invoke(challengeClass, "getValue", new File("path/file name.png")));
		assertEquals("file", Classes.invoke(challengeClass, "getValue", new File("path/file.png")));
		assertEquals("file", Classes.invoke(challengeClass, "getValue", new File("path/file")));
		assertEquals("file", Classes.invoke(challengeClass, "getValue", new File("file")));
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	private static Object config(Object... args) throws Exception {
		if (args.length == 0) {
			args = new Object[] { null };
		}

		String config = "" + //
				"<captcha>" + //
				"	<property name='captcha.repository.path' value='%s' />" + //
				"	<property name='captcha.set.size' value='%d' />" + //
				"</captcha>";

		String path = args[0] instanceof File ? ((File) args[0]).getAbsolutePath() : (String) args[0];
		int size = 6;
		if (args.length > 1) {
			size = (Integer) args[1];
		}
		if (args.length < 3) {
			return captcha(String.format(config, path, size), null);
		}
		return captcha(String.format(config, path, size), (AppContext) args[2]);
	}

	private static Object captcha(String config, AppContext context) throws Exception {
		ConfigBuilder builder = new ConfigBuilder(config);
		Configurable captcha = Classes.newInstance(CAPTCHA, context);
		captcha.config(builder.build());
		return captcha;
	}
}
