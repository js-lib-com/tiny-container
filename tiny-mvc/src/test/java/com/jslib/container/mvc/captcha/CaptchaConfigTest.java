package com.jslib.container.mvc.captcha;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.container.spi.IContainer;
import com.jslib.lang.ConfigBuilder;
import com.jslib.lang.ConfigException;

@RunWith(MockitoJUnitRunner.class)
public class CaptchaConfigTest {
	@Mock
	private IContainer container;

	private Captcha captcha;

	@Before
	public void beforeTest() {
		captcha = new Captcha(container);
	}

	@Test
	public void GivenValidDescriptor_WhenConfig_ThenInternalStateInitialized() throws Exception {
		String config = "" + //
				"<captcha>" + //
				"	<property name='captcha.repository.path' value='src/test/resources/captcha' />" + //
				"	<property name='captcha.set.size' value='5' />" + //
				"</captcha>";

		// when
		captcha.config(new ConfigBuilder(config).build());

		// then
		assertThat(captcha.imagesRepositoryDir(), notNullValue());
		assertThat(captcha.challengeSetSize(), equalTo(5));
	}

	@Test(expected = ConfigException.class)
	public void GivenMissingRepositoryPathAttribute_WhenConfig_ThenException() throws Exception {
		// given
		// attribute captcha.repository.dir is wrong; it should be captcha.repository.path
		String config = "" + //
				"<captcha>" + //
				"	<property name='captcha.repository.dir' value='fixture/captcha' />" + //
				"</captcha>";

		// when
		captcha.config(new ConfigBuilder(config).build());

		// then
	}

	@Test(expected = ConfigException.class)
	public void GivenNotExistingRepository_WhenConfig_ThenException() throws Exception {
		// given
		String config = "" + //
				"<captcha>" + //
				"	<property name='captcha.repository.path' value='fake-captcha' />" + //
				"	<property name='captcha.set.size' value='5' />" + //
				"</captcha>";

		// when
		captcha.config(new ConfigBuilder(config).build());

		// then
	}

	@Test(expected = ConfigException.class)
	public void GivenRepositoryNotDirectory_WhenConfig_ThenException() throws Exception {
		// given
		String config = "" + //
				"<captcha>" + //
				"	<property name='captcha.repository.path' value='src/test/resources/captcha/apple.png' />" + //
				"	<property name='captcha.set.size' value='5' />" + //
				"</captcha>";

		// when
		captcha.config(new ConfigBuilder(config).build());

		// then
	}

	@Test(expected = ConfigException.class)
	public void GivenEmptyRepository_WhenConfig_ThenException() throws Exception {
		String config = "" + //
				"<captcha>" + //
				"	<property name='captcha.repository.path' value='src/test/resources/empty' />" + //
				"	<property name='captcha.set.size' value='10' />" + //
				"</captcha>";

		// when
		captcha.config(new ConfigBuilder(config).build());

		// then
	}

	@Test(expected = ConfigException.class)
	public void GivenSetSizeLargerThanImageFilesCount_WhenConfig_ThenException() throws Exception {
		// given
		String config = "" + //
				"<captcha>" + //
				"	<property name='captcha.repository.path' value='src/test/resources/captcha' />" + //
				"	<property name='captcha.set.size' value='10' />" + //
				"</captcha>";

		// when
		captcha.config(new ConfigBuilder(config).build());

		// then
	}
}
