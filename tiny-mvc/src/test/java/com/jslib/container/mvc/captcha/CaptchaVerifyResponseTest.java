package com.jslib.container.mvc.captcha;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.container.servlet.RequestContext;
import com.jslib.container.spi.IContainer;
import com.jslib.lang.ConfigBuilder;
import com.jslib.util.Classes;
import com.jslib.util.Files;
import com.jslib.util.Strings;

import jakarta.servlet.http.HttpSession;

@RunWith(MockitoJUnitRunner.class)
public class CaptchaVerifyResponseTest {
	@Mock
	private IContainer container;
	@Mock
	private RequestContext requestContext;
	@Mock
	private HttpSession httpSession;

	private Captcha captcha;

	@Before
	public void beforeTest() {
		when(container.getInstance(RequestContext.class)).thenReturn(requestContext);
		when(requestContext.getSession(true)).thenReturn(httpSession);
		when(httpSession.getAttribute("challenges-key")).thenReturn(new HashMap<Integer, Challenge>());

		captcha = new Captcha(container);
	}

	@Test
	public void GivenCaptcha_WhenGetChallenge_ThenVerifyChallengeResponse() throws Exception {
		// given
		String config = "" + //
				"<captcha>" + //
				"	<property name='captcha.repository.path' value='src/test/resources/captcha' />" + //
				"	<property name='captcha.set.size' value='5' />" + //
				"</captcha>";
		captcha.config(new ConfigBuilder(config).build());
		
		// when
		Challenge challenge = captcha.getChallenge(0);

		// then
		for (Map.Entry<String, File> entry : challenge.tokenedImageFiles().entrySet()) {
			if (Files.basename(entry.getValue()).equals(challenge.value())) {
				assertThat(captcha.verifyResponse(0, entry.getKey()), nullValue());
				break;
			}
		}
		assertThat(captcha.verifyResponse(0, Strings.UUID()), notNullValue());
	}

	@Test
	public void GivenCaptcha_WhenGetMultipleChallenges_ThenVerifyEveryChallengeResponse() throws Exception {
		// given
		String config = "" + //
				"<captcha>" + //
				"	<property name='captcha.repository.path' value='src/test/resources/captcha' />" + //
				"	<property name='captcha.set.size' value='5' />" + //
				"</captcha>";
		captcha.config(new ConfigBuilder(config).build());

		for (int i = 0; i < 100; ++i) {
			// when
			Challenge challenge = captcha.getChallenge(i);
			
			// then
			for (Map.Entry<String, File> entry : challenge.tokenedImageFiles().entrySet()) {
				if (Files.basename(entry.getValue()).equals(challenge.value())) {
					assertNull(Classes.invoke(captcha, "verifyResponse", i, entry.getKey()));
					break;
				}
			}
			assertThat(captcha.verifyResponse(i, Strings.UUID()), notNullValue());
		}
	}

	/** Invoking challenge response verify with no challenge on session should rise illegal state. */
	@Test(expected = IllegalStateException.class)
	public void GivenNoChallenge_WhenVerifyCaptchaResponse_ThenException() throws Exception {
		// given
		String config = "" + //
				"<captcha>" + //
				"	<property name='captcha.repository.path' value='src/test/resources/captcha' />" + //
				"	<property name='captcha.set.size' value='5' />" + //
				"</captcha>";
		captcha.config(new ConfigBuilder(config).build());

		// when
		captcha.verifyResponse(0, Strings.UUID());

		// then
	}

	/** Empty token on challenge response verify should rise illegal argument. */
	@Test(expected = IllegalArgumentException.class)
	public void GivenEmptyToken_WhenVerifyCaptchaResponse_ThenException() throws Exception {
		captcha.verifyResponse(0, "");
	}

	/** Null token on challenge response verify should rise illegal argument. */
	@Test(expected = IllegalArgumentException.class)
	public void GivenNullToken_WhenVerifyCaptchaResponse_ThenException() throws Exception {
		captcha.verifyResponse(0, null);
	}
}
