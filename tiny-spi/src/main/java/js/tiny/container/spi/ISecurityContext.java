package js.tiny.container.spi;

import java.security.Principal;

import jakarta.ws.rs.core.SecurityContext;
import js.lang.BugError;

/**
 * Security context is an unified interface for both servlet container and application provided authentication. There are two
 * login method variants: one for servlet container provided authentication, see {@link #login(String, String)} and the second
 * used when authentication is implemented by application, {@link #authenticate(Principal)}. Security context implementation should
 * adapt behavior considering login variant.
 * 
 * Basically, a security context has means to login and logout and test if is currently authenticated. On successful login
 * implementation creates a principal and store it on this security context. Stored principal can be retrieved, see
 * {@link #getUserPrincipal()}.
 * 
 * Security context can be injected into application classes. Anyway, since security context implementation depends on HTTP
 * session it can be used only inside business methods, in a valid HTTP request context. Attempting to use security context
 * outside HTTP request will throw exception, e.g. post construct or timer methods.
 * 
 * @author Iulian Rotaru
 */
public interface ISecurityContext extends SecurityContext {
	/**
	 * Authenticates the provided username and password and binds the authenticated principal to this security context. Use this
	 * login variant when authentication is provided by servlet container. Implementation should delegate servlet container
	 * login services.
	 * 
	 * @param username user name,
	 * @param password user password.
	 * @return true if authentication succeed.
	 */
	boolean login(String username, String password);

	/**
	 * Bind application authenticated principal to this security context. This login variant is used when authentication is
	 * provided by application. Given principal is already authenticated by application logic and there is no reason to fail.
	 * Therefore implementation should not throw any exception.
	 * 
	 * @param principal application authenticated user principal.
	 */
	void authenticate(Principal principal);

	/**
	 * Remove authenticated user from this security context. After executing this method security context become not
	 * authenticated and {@link #getUserPrincipal()} always returns null.
	 * <p>
	 * If authentication on login was provided by servlet container implementation should inform it about this logout event,
	 * beside updating implementation own state.
	 */
	void logout();

	/**
	 * Get authenticated principal for this security context. A security context become authenticated, and has a principal,
	 * after successful {@link #login(String, String)} or {@link #authenticate(Principal)}. Returns null if this security context is
	 * not authenticated.
	 * 
	 * @return this security context authenticated principal or null if none.
	 */
	Principal getUserPrincipal();

	/**
	 * Test if this security context is authenticated. A security context is authenticated if has bound an authenticated
	 * principal. This predicate is a convenient alternative for <code>getUserPrincipal() != null</code>.
	 * 
	 * @return true if current security context is authenticated.
	 */
	boolean isAuthenticated();

	/**
	 * Check if this security context is authorized for any of the requested roles. First of all for this security context to be
	 * authorized it must be authenticated. This predicate should always return false if {@link #getUserPrincipal()} returns
	 * null or {@link #isAuthenticated()} return false.
	 * <p>
	 * If <code>roles</code> argument is empty this predicate should behave exactly like {@link #isAuthenticated()}, that is, it
	 * should return true if there is user principal on context. If more roles are provided implementation should return true if
	 * any of them is authorized.
	 * 
	 * @param roles optional and variable number of roles against to check authentication.
	 * @return true if current security context is authorized.
	 * @throws BugError if <code>roles</code> argument is null.
	 */
	boolean isAuthorized(String... roles);
}
