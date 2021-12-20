package js.tiny.container.servlet;

import js.converter.ConverterException;

public interface WebContext {

	/**
	 * Get application name. Returned application name is rather for application identification than for displaying on user
	 * interface.
	 * 
	 * @return application name.
	 */
	String getAppName();

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

	
	<T> T getInstance(Class<T> interfaceClass);
	
	RequestContext getRequestContext(); 
	
	SecurityContext getSecurityContext();
	
}
