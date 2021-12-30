package js.tiny.container.servlet;

import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.ISecurityContext;

public interface ITinyContainer extends IContainer, ISecurityContext {

	/**
	 * Get application name. Returned application name is rather for application identification than for displaying on user
	 * interface.
	 * 
	 * @return application name.
	 */
	String getAppName();

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
