package com.jslib.container.mvc.captcha;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.container.servlet.RequestContext;
import com.jslib.container.spi.IContainer;
import com.jslib.lang.BugError;
import com.jslib.lang.ConfigBuilder;
import com.jslib.util.Files;

import jakarta.servlet.http.HttpSession;

@RunWith(MockitoJUnitRunner.class)
public class CaptchaChallengeTest {
	@Mock
	private IContainer container;
	@Mock
	private RequestContext requestContext;
	@Mock
	private HttpSession httpSession;

	private File repository;
	private Captcha captcha;

	@Before
	public void beforeTest() {
		when(container.getInstance(RequestContext.class)).thenReturn(requestContext);
		when(requestContext.getSession(true)).thenReturn(httpSession);

		repository = new File("src/test/resources/captcha");
		captcha = new Captcha(container);
	}

	/**
	 * Challenge contains two fields visible on client script by name. These names should not be changed and this test ensure
	 * that. Also challenge images name is a request to a dynamic resource with format <code>captcha/image?token</code>.
	 */
	@Test
	public void GivenValidRespository_WhenConstructor_ThenValidInternalState() {
		// given
		int setSize = 3;
		
		// when
		Challenge challenge = new Challenge(repository, setSize);

		// then
		List<String> images = challenge.images();
		assertNotNull(images);
		assertEquals(setSize, images.size());
		for (String image : images) {
			assertTrue(image.startsWith("captcha/image?"));
		}

		String value = challenge.value();
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

	/** CAPTCHA challenge getter should create a new challenge and store on HTTP session challenges store. */
	@Test
	public void GivenValidConfiguration_WhenGetChallenge_ThenChallengeStoredOnSession() throws Exception {
		// given
		when(httpSession.getAttribute("challenges-key")).thenReturn(new HashMap<Integer, Challenge>());

		String config = "" + //
				"<captcha>" + //
				"	<property name='captcha.repository.path' value='src/test/resources/captcha' />" + //
				"	<property name='captcha.set.size' value='5' />" + //
				"</captcha>";
		captcha.config(new ConfigBuilder(config).build());

		// when
		Challenge challenge = captcha.getChallenge(0);

		// then
		assertThat(challenge, notNullValue());
		assertThat(challenge.value(), notNullValue());
		assertThat(challenge.tokenedImageFiles(), notNullValue());

		// assert challenges store from HTTP session contains the newly created challenge
		Map<Integer, Challenge> challenges = captcha.getChallenges();
		assertThat(challenges.get(0), equalTo(challenge));
		
		verify(httpSession, times(2)).getAttribute("challenges-key");
	}

	@Test(expected = BugError.class)
	public void GivenRepositoryNotConfigured_WhenGetChallenge_ThenException() throws Exception {
		// given

		// when
		captcha.getChallenge(0);

		// then
	}

	@Test
	public void GivenValidToken_WhenVerifyResponse_Thentrue() throws Exception {
		// given
		Challenge challenge = new Challenge(repository, 1);
		Map<String, File> tokenedImageFiles = challenge.tokenedImageFiles();
		String token = tokenedImageFiles.keySet().iterator().next();

		// when
		boolean result = challenge.verifyResponse(token);

		// then
		assertTrue(result);
	}

	@Test
	public void GivenFakeToken_WhenVerifyResponse_ThenFalse() throws Exception {
		// given
		Challenge challenge = new Challenge(repository, 1);

		// when
		boolean result = challenge.verifyResponse("fake-token");

		assertFalse(result);
	}

	@Test
	public void GivenEmptyToken_WhenVerifyResponse_ThenFalse() throws Exception {
		// given
		Challenge challenge = new Challenge(repository, 1);

		// when
		boolean result = challenge.verifyResponse("");

		assertFalse(result);
	}

	@Test
	public void GivenNullToken_WhenVerifyResponse_ThenFalse() throws Exception {
		// given
		Challenge challenge = new Challenge(repository, 1);

		// when
		boolean result = challenge.verifyResponse(null);

		assertFalse(result);
	}

	@Test
	public void GivenValidFile_WhenGetValue_ThenValue() throws Exception {
		assertEquals("file name", Challenge.getValue(new File("path/file-name.png")));
		assertEquals("file name", Challenge.getValue(new File("path/file.name.png")));
		assertEquals("file name", Challenge.getValue(new File("path/file_name.png")));
		assertEquals("file name", Challenge.getValue(new File("path/file name.png")));
		assertEquals("file", Challenge.getValue(new File("path/file.png")));
		assertEquals("file", Challenge.getValue(new File("path/file")));
		assertEquals("file", Challenge.getValue(new File("file")));
	}

	@Test
	public void GivenValidToken_WhenGetImage_ThenNotNull() throws Exception {
		// given
		Challenge challenge = new Challenge(repository, 3);
		Map<String, File> tokenedImageFiles = challenge.tokenedImageFiles();
		String token = tokenedImageFiles.keySet().iterator().next();

		// when
		File image = challenge.getImage(token);

		// then
		assertThat(image, notNullValue());
		assertThat(image, equalTo(tokenedImageFiles.get(token)));
	}

	@Test
	public void GivenNullToken_WhenGetImage_ThenNull() throws Exception {
		// given
		Challenge challenge = new Challenge(repository, 3);

		// when
		File image = challenge.getImage(null);

		// then
		assertThat(image, nullValue());
	}

	@Test
	public void GivenEmptyToken_WhenGetImage_ThenNull() throws Exception {
		// given
		Challenge challenge = new Challenge(repository, 3);

		// when
		File image = challenge.getImage("");

		// then
		assertThat(image, nullValue());
	}
}
