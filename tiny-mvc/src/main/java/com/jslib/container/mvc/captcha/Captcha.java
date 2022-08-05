package com.jslib.container.mvc.captcha;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.container.http.NoSuchResourceException;
import com.jslib.container.http.Resource;
import com.jslib.container.mvc.FileResource;
import com.jslib.container.mvc.ResourceServlet;
import com.jslib.container.servlet.RequestContext;
import com.jslib.container.spi.IContainer;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.ejb.Remote;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.Path;
import com.jslib.lang.BugError;
import com.jslib.lang.Config;
import com.jslib.lang.ConfigException;
import com.jslib.util.Params;

/**
 * Implementation of Simple CAPTCHA service. This implementation provide services to <code>captcha</code> widget from script
 * library, see <a href="http://api.js-lib.com/widget/js/widget/Captcha.html">widget API</a>.
 * <p>
 * An application using Simple CAPTCHA needs to configure it via application and deployment descriptors.
 * 
 * There are only two properties: images repository path and images set size. Images repository is not required to be public. In
 * fact is good practice to hide it on private file system, of course accessible by web server.
 * 
 * <pre>
 * &lt;captcha&gt;
 * 	&lt;property name="captcha.repository.path" value="/usr/share/captcha" /&gt;
 * 	&lt;property name="captcha.set.size" value="5" /&gt;
 * &lt;/captcha&gt;
 * </pre>
 * <p>
 * Images repository is mandatory and should be an existing absolute path. Repository should contain only images related to
 * CAPTCHA. Sub-directories are not recommended, they are skipped anyway. Challenge set size is optional with default value 6.
 * <p>
 * Challenge images are not loaded by client explicitly by name but via an unique token, usable only once. URL for images is
 * always of form <code>.../captcha/image?token</code> and is served by {@link ResourceServlet}. An application that used Simple
 * CAPTCHA need to enable resource servlet and route <code>/captcha/*</code> requests to it.
 * 
 * <pre>
 * &lt;servlet-mapping&gt;
 * 	&lt;servlet-name&gt;xsp-servlet&lt;/servlet-name&gt;
 * 	&lt;url-pattern&gt;/captcha/*&lt;/url-pattern&gt;
 * &lt;/servlet-mapping&gt;
 * </pre>
 * 
 * @author Iulian Rotaru
 * @version final
 */
@Remote
@Path("captcha")
@PermitAll
final class Captcha {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(Captcha.class);

	/** Web session key name for stored challenge instances. */
	private static final String CHALENGES_KEY = "challenges-key";

	/** Parent container back reference. */
	private final IContainer container;

	/** Challenge images directory configured from application descriptor. */
	private File imagesRepositoryDir;

	/** The number of challenge images that are displayed at once, default to 6. Configurable from application descriptor. */
	private int challengeSetSize;

	/**
	 * Create CAPTCHA manager instance with injected application context.
	 * 
	 * @param container parent container back reference.
	 */
	public Captcha(IContainer container) {
		this.container = container;
	}

	/**
	 * Configure Simple CAPTCHA service provider. See class description for configuration object properties.
	 * <p>
	 * Note that images repository should contain only images related to CAPTCHA; sub-directories are not scanned, only direct
	 * child files.
	 * 
	 * @param config configuration section from application descriptor.
	 */
	@DenyAll
	public void config(Config config) throws ConfigException {
		imagesRepositoryDir = config.getProperty("captcha.repository.path", File.class);
		if (imagesRepositoryDir == null) {
			throw new ConfigException("Missing <captcha.repository.path> property from CAPTCHA configuration.");
		}
		challengeSetSize = config.getProperty("captcha.set.size", int.class, 6);

		if (!imagesRepositoryDir.exists()) {
			throw new ConfigException("CAPTCHA images repository |%s| does not exist.", imagesRepositoryDir);
		}
		if (!imagesRepositoryDir.isDirectory()) {
			throw new ConfigException("CAPTCHA images repository |%s| is not a directory.", imagesRepositoryDir);
		}

		int imagesCount = imagesRepositoryDir.list().length;
		if (imagesCount == 0) {
			throw new ConfigException("CAPTCHA images repository |%s| is empty.", imagesRepositoryDir);
		}
		if (imagesCount <= challengeSetSize) {
			throw new ConfigException("Challenge set size is larger that avaliable images count from CAPTCHA repository.");
		}
	}

	/**
	 * Create a new challenge and store it on HTTP session. If HTTP session does not exist, create it. This challenge is used by
	 * client to update user interface accordingly. It is called on initial form rendering and every time user choose to load
	 * another challenge if current one cannot be solved.
	 * <p>
	 * Client is free to display multiple Simple CAPTCHA instances but all instances should have distinct index, generated in
	 * sequence, starting from zero.
	 * 
	 * @param captchaIndex CAPTCHA instance index,
	 * @return newly created CAPTCHA challenge.
	 * @throws BugError if this CAPTCHA manager is not properly initialized.
	 */
	public Challenge getChallenge(int captchaIndex) {
		if (imagesRepositoryDir == null) {
			log.debug("Simple CAPTCHA not properly initialized. Missing <captcha> section from application descriptor:\r\n" + //
					"\t<captcha>\r\n" + //
					"\t\t<property name=\"captcha.repository.path\" value=\"/path/to/captcha/images\" />\r\n" + //
					"\t\t<property name=\"captcha.set.size\" value=\"5\" />\r\n" + //
					"\t</captcha>");
			throw new BugError("Missing CAPTCHA images repository. Most probably <captcha> section is missing from application descriptor.");
		}

		Challenge challenge = new Challenge(imagesRepositoryDir, challengeSetSize);
		getChallenges().put(captchaIndex, challenge);
		return challenge;
	}

	/**
	 * Verify challenge response. If response is not correct returns a new challenge used by client to update user interface. It
	 * is highly recommended to refresh challenge on wrong answer to prevent response guessing.
	 * <p>
	 * Instance index should be the same provided to {@link #getChallenge(int)}. This is critical and client should consider
	 * this constrain. Anyway, using wrong CAPTCHA instance index is not a security breach; it always consider response as
	 * invalid.
	 * 
	 * @param captchaIndex CAPTCHA instance index,
	 * @param challengeResponse challenge response from client.
	 * @return null if response is correct or a new challenge.
	 * @throws IllegalArgumentException if given challenge response is null or empty.
	 * @throws IllegalStateException if there is no challenge on session.
	 */
	public Challenge verifyResponse(int captchaIndex, String challengeResponse) throws IllegalArgumentException, IllegalStateException {
		Params.notNullOrEmpty(challengeResponse, "Challenge response");
		Challenge challenge = getChallenges().get(captchaIndex);
		if (challenge == null) {
			throw new IllegalStateException("Invalid challenge on session.");
		}
		return challenge.verifyResponse(challengeResponse) ? null : getChallenge(captchaIndex);
	}

	/**
	 * Get the image identified by given token so that client is able to display it. In order to protect against recording
	 * attack, a challenge image is identified by a token that is valid only once.
	 * <p>
	 * If given token does not identify a valid challenge image this method throws {@link NoSuchResourceException} and container
	 * responds with 404 Not Found.
	 * 
	 * @param token challenge image identifier.
	 * @return requested challenge image.
	 * @throws IllegalArgumentException if <code>token</code> argument is null or empty.
	 * @throws NoSuchResourceException if there is no challenge on session or token does not identify a challenge image.
	 */
	@Path("image")
	public Resource getImage(String token) throws IllegalArgumentException, NoSuchResourceException {
		Params.notNullOrEmpty(token, "Image token");
		for (Challenge challenge : getChallenges().values()) {
			File image = challenge.getImage(token);
			if (image != null) {
				return new FileResource(image);
			}
		}
		throw new NoSuchResourceException();
	}

	/**
	 * Get challenge instances storage bound to current HTTP request. Challenges storage is kept on HTTP session. It is legal to
	 * have multiple Simple CAPTCHA instances on a single page but client should use zero based numeric index to identify
	 * instances.
	 * <p>
	 * This method has side effects: it creates HTTP session if is not already created.
	 * 
	 * @return challenge instances storage.
	 * @throws IllegalStateException if attempt to create new session after response commit.
	 */
	Map<Integer, Challenge> getChallenges() {
		// here is a circular package dependency that is hard to avoid
		// js.servlet package depends on js.http packages and this js.http.captcha package depends on js.servlet
		// as a consequence js.http.captcha package cannot be used externally without js.servlet
		HttpSession session = container.getInstance(RequestContext.class).getSession(true);

		@SuppressWarnings("unchecked")
		Map<Integer, Challenge> challenges = (Map<Integer, Challenge>) session.getAttribute(CHALENGES_KEY);
		if (challenges == null) {
			challenges = new HashMap<>();
			session.setAttribute(CHALENGES_KEY, challenges);
		}
		return challenges;
	}

	// --------------------------------------------------------------------------------------------

	File imagesRepositoryDir() {
		return imagesRepositoryDir;
	}

	int challengeSetSize() {
		return challengeSetSize;
	}
}
