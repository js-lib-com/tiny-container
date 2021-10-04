package js.tiny.container.servlet;

import js.tiny.container.spi.IContainer;

public interface ITinyContainer extends IContainer {

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
	
}
