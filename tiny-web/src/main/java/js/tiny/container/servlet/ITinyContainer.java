package js.tiny.container.servlet;

import js.converter.ConverterException;
import js.tiny.container.spi.IContainer;

public interface ITinyContainer extends IContainer, SecurityContext {

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
