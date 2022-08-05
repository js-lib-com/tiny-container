package com.jslib.container.http.form;

import java.io.InputStream;

/**
 * Base class for user defined handler used by form <em>for each</em> iterator. While iterating a form using
 * {@link FormIterator#forEach(FormHandler)} caller supplies an implementation of this abstract class, overriding necessary
 * method(s).
 * <p>
 * There are two kinds of form parts, streams and fields and form handler provides callback method for every kind.
 * 
 * <pre>
 * formIterator.forEach(new FormHandler()
 * {
 *   &#064;Override
 *   protected void stream(String fieldName, String fileName, String contentType, InputStream inputStream) {
 *      ... 
 *   }
 * 
 *   &#064;Override
 *   protected void field(String name, String value) {
 *     ...
 *   }
 * });
 * </pre>
 * 
 * Since this class provide NOP implementations it is not mandatory to override both methods. One may have, for example, a form
 * only with uploads and override only {@link #stream(String, String, String, InputStream)} method.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public abstract class FormHandler {
	/**
	 * Callback method invoked for a form part carrying a stream. Note that implementation takes care to close input stream
	 * after returning from this callback, with success or exception. Also part name is guaranteed to be Java member name like,
	 * even if on underlying multipart form is dashed name.
	 * 
	 * @param name normalized part name,
	 * @param fileName file name as designated on client host,
	 * @param contentType part content type,
	 * @param inputStream part input stream.
	 * @throws Throwable if used defined logic fails for whatever reason.
	 */
	protected void stream(String name, String fileName, String contentType, InputStream inputStream) throws Throwable {
	}

	/**
	 * Callback method invoked for a form field. Note that part name is guaranteed to be Java member name like, even if on
	 * client was dashed name.
	 * 
	 * @param name normalized field part name,
	 * @param value field value.
	 * @throws Throwable if used defined logic fails for whatever reason.
	 */
	protected void field(String name, String value) throws Throwable {
	}
}
