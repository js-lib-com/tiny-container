package js.core;

import java.io.File;
import java.util.Locale;

import js.converter.ConverterException;

/**
 * Application context provides access to container services. There are two major services inherited from interfaces: managed
 * instance retrieval and security context related operations. Beside these there are couple handy utility methods.
 * <p>
 * Application context instance can be injected into managed classes or retrieved from application factory.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface AppContext extends AppFactory, SecurityContext {
	/**
	 * Get application name. Returned application name is rather for application identification than for displaying on user
	 * interface.
	 * 
	 * @return application name.
	 */
	String getAppName();

	/**
	 * Get absolute file for a path relative to application private storage.
	 * 
	 * @param path path relative to application private storage.
	 * @return application private file.
	 */
	File getAppFile(String path);

	/**
	 * Get context property. A context property is defined by means external to application, on run-time environment.
	 * 
	 * @param name context property name.
	 * @return context property value, null if not defined.
	 */
	String getProperty(String name);

	/**
	 * Alternative of {@link #getProperty(String)} that convert string property to requested type.
	 * 
	 * @param name context property name,
	 * @param type requested type.
	 * @param <T> context property type.
	 * @return context property value converted to requested type or null if property not defined.
	 * @throws ConverterException if property exist but cannot be converted to requested type.
	 */
	<T> T getProperty(String name, Class<T> type);

	/**
	 * Get current request preferred locale that the client will accept content in, based on the <code>Accept-Language</code>
	 * header. If the client request does not provide an <code>Accept-Language</code> header, this method returns the default
	 * locale for the server. Current request is that bound to current thread.
	 * 
	 * @return current request locale.
	 */
	Locale getRequestLocale();

	/**
	 * Returns the Internet Protocol (IP) address of the client or last proxy that sent current request. Current request is that
	 * bound to current thread.
	 * 
	 * @return current request remote address.
	 */
	String getRemoteAddr();
}
