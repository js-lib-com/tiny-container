package js.tiny.container.servlet;

import java.security.Principal;

import js.tiny.container.core.SecurityContext;

/**
 * A nonce user has access only one time and for a short period of time. Container takes care to quickly invalidate the session.
 * Default value for session duration is 10 seconds.
 * <p>
 * An example use case would be granting guest access to private resources based on a coupon or credit points of some sort.
 * Guest user is requested to enter coupon code; application checks coupon validity then creates a <code>nonce</code> principal
 * and authenticate it via {@link SecurityContext#login(Principal)}. Security context takes care to auto-logout
 * <code>nonce</code> principal after a short inactivity interval, declared at <code>nonce</code> creation.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public class NonceUser implements Principal {
	/** Nonce user name. */
	private static final String NONCE_NAME = "nonce";

	/**
	 * Maximum inactivity interval, in seconds, initialed by constructor. After this interval expires security context does
	 * auto-logout.
	 */
	private final int maxInactiveInterval;

	/**
	 * Create <code>nonce</code> principal with specified value for inactivity interval. If inactivity interval expires security
	 * context executes auto-logout.
	 * 
	 * @param maxInactiveInterval value for maximum inactivity interval, expressed in seconds.
	 */
	public NonceUser(int maxInactiveInterval) {
		this.maxInactiveInterval = maxInactiveInterval;
	}

	@Override
	public String getName() {
		return NONCE_NAME;
	}

	/**
	 * Get value of maximum inactivity interval, in seconds, configured for this <code>nonce</code> principal.
	 * 
	 * @return maximum inactivity interval, expressed in seconds.
	 * @see maxInactiveInterval
	 */
	public int getMaxInactiveInterval() {
		return maxInactiveInterval;
	}
}
