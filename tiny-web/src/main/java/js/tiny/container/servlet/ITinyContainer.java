package js.tiny.container.servlet;

import java.io.File;
import java.util.Locale;

import js.converter.ConverterException;
import js.tiny.container.spi.IContainer;

public interface ITinyContainer extends IContainer, SecurityContext {

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

	/**
	 * Get basic authentication realm. If realm is not defined into application descriptor uses context name.
	 * <p>
	 * Basic authentication realm is loaded from application descriptor, <code>login</code> section.
	 * 
	 * <pre>
	 * &lt;login&gt;
	 * 	&lt;property name="realm" value="Fax2e-mail" /&gt;
	 * 	...
	 * &lt;/login&gt;
	 * </pre>
	 * 
	 * @return basic authentication realm
	 */
	String getLoginRealm();

	/**
	 * Get location for application login page, relative or absolute to servlet container root.
	 * <p>
	 * Login page location is loaded from application descriptor, <code>login</code> section.
	 * 
	 * <pre>
	 * &lt;login&gt;
	 * 	...
	 * 	&lt;property name="page" value="index.htm" /&gt;
	 * &lt;/login&gt;
	 * </pre>
	 * 
	 * @return login page location or null if not login page declared.
	 */
	String getLoginPage();

	/**
	 * Get context property converted to requested type. A context property is defined by means external to application, on
	 * run-time environment.
	 * 
	 * @param name context property name,
	 * @param type requested type.
	 * @param <T> context property type.
	 * @return context property value converted to requested type or null if property not defined.
	 * @throws ConverterException if property exist but cannot be converted to requested type.
	 */
	<T> T getProperty(String name, Class<T> type);
}
