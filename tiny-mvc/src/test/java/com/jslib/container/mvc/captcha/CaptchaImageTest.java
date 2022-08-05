package com.jslib.container.mvc.captcha;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.container.http.NoSuchResourceException;
import com.jslib.container.http.Resource;
import com.jslib.container.servlet.RequestContext;
import com.jslib.container.spi.IContainer;
import com.jslib.lang.ConfigBuilder;
import com.jslib.lang.ConfigException;
import com.jslib.util.Strings;

import jakarta.servlet.http.HttpSession;

@RunWith(MockitoJUnitRunner.class)
public class CaptchaImageTest {
	@Mock
	private IContainer container;
	@Mock
	private RequestContext requestContext;
	@Mock
	private HttpSession httpSession;

	private Captcha captcha;

	@Before
	public void beforeTest() throws ConfigException {
		when(container.getInstance(RequestContext.class)).thenReturn(requestContext);
		when(requestContext.getSession(true)).thenReturn(httpSession);
		when(httpSession.getAttribute("challenges-key")).thenReturn(new HashMap<Integer, Challenge>());

		String config = "" + //
				"<captcha>" + //
				"	<property name='captcha.repository.path' value='src/test/resources/captcha' />" + //
				"	<property name='captcha.set.size' value='5' />" + //
				"</captcha>";
		captcha = new Captcha(container);
		captcha.config(new ConfigBuilder(config).build());
	}

	/** Get challenge image test with a large number of CAPTCHA instances. */
	@Test
	public void GivenMultipleChallenges_WhenCaptchaGetImage_ThenNotNull() throws Exception {
		// given
		List<String> tokens = new ArrayList<>();
		for (int i = 0; i < 100; ++i) {
			Challenge challenge = captcha.getChallenge(i);
			for (String token : challenge.tokenedImageFiles().keySet()) {
				tokens.add(token);
			}
		}

		for (String token : tokens) {
			// when
			Resource image = captcha.getImage(token);

			// then
			assertThat(image, notNullValue());
		}
	}

	/** Missing challenge from session is considered resource not found. */
	@Test(expected = NoSuchResourceException.class)
	public void GivenMissingChallenge_WhenCaptchaGetImage_ThenException() throws Exception {
		// given
		// do not call captcha.getChallenge(0); need to simulate missing challenge
		// captcha.getChallenge(0);

		// when
		captcha.getImage(Strings.UUID());

		// then
	}

	@Test(expected = NoSuchResourceException.class)
	public void GivenCaptchaChallenge_WhenCaptchaGetImageWithNotExistingToken_ThenException() throws Exception {
		// given
		captcha.getChallenge(0);

		// when
		captcha.getImage(Strings.UUID());

		// then
	}

	/**
	 * CAPTCHA challenge image is retrieved after challenge creation. Test that all tokens from challenges set have related
	 * image.
	 */
	@Test
	public void GivenChallenge_WhenCaptchaGetImageForEveryToken_ThenNotNull() throws Exception {
		// given
		Challenge challenge = captcha.getChallenge(0);

		for (String token : challenge.tokenedImageFiles().keySet()) {
			// when
			Resource image = captcha.getImage(token);
			
			// then
			assertThat(image, notNullValue());
		}
	}
}
