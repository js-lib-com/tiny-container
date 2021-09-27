package js.tiny.container.http.encoder;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.servlet.http.HttpServletRequest;

import js.lang.IllegalArgumentException;
import js.tiny.container.http.form.Form;
import js.tiny.container.http.form.FormImpl;
import js.tiny.container.http.form.FormIterator;
import js.tiny.container.http.form.FormIteratorImpl;
import js.tiny.container.http.form.FormObject;
import js.tiny.container.http.form.Part;
import js.tiny.container.http.form.UploadStream;
import js.tiny.container.http.form.UploadedFile;
import js.util.Files;

/**
 * Net method invocation arguments reader for multipart form. This arguments reader expects a multipart form on HTTP request. If
 * there is no valid form on request throws illegal arguments. Parses the form field(s), create an instance of requested formal
 * type and fill it with fields value(s).
 * <p>
 * This arguments reader is used only for methods with single argument; if given formal parameters has not a single item throws
 * illegal argument exception. Current implementation recognizes next types: {@link Form}, {@link FormIterator},
 * {@link UploadedFile}, {@link UploadStream}, {@link InputStream} and {@link FormObject}.
 * <p>
 * This arguments reader implementation is heavily based on {@link js.tiny.container.http.form} package.
 * 
 * @author Iulian Rotaru
 * @version final
 */
final class MultipartFormArgumentsReader implements ArgumentsReader {
	/**
	 * Store stream argument, if any, so that to be able to close it after method execution. Do not store stream argument as
	 * field of this arguments reader since instance is reused and cannot have state.
	 */
	private final ThreadLocal<Closeable> threadLocal = new ThreadLocal<Closeable>();

	/**
	 * Parse multipart form from HTTP request, create object instance of requested type and fill it with form field value(s). If
	 * formal type is a stream store it to local thread so that to be able to close it after arguments processed by application
	 * code.
	 * 
	 * @param httpRequest HTTP request,
	 * @param formalParameters formal parameter types.
	 * @return single item arguments array.
	 * @throws IOException if reading from HTTP request input stream fails.
	 * @throws IllegalArgumentException if formal parameters does not contain exactly one type.
	 * @throws IllegalArgumentException if formal parameter type is parameterized.
	 * @throws IllegalArgumentException if HTTP request does not contain a valid multipart form.
	 */
	@Override
	public Object[] read(HttpServletRequest httpRequest, Type[] formalParameters) throws IOException, IllegalArgumentException {
		if (formalParameters.length != 1) {
			throw new IllegalArgumentException("Bad parameters count. Should be exactly one but is |%d|.", formalParameters.length);
		}
		if (formalParameters[0] instanceof ParameterizedType) {
			throw new IllegalArgumentException("Parameterized type |%s| is not supported.", formalParameters[0]);
		}

		Class<?> type = (Class<?>) formalParameters[0];
		Object[] arguments = new Object[1];

		if (type.equals(Form.class)) {
			arguments[0] = new FormImpl(httpRequest);
		}

		else if (type.equals(FormIterator.class)) {
			arguments[0] = new FormIteratorImpl(httpRequest);
		}

		else if (type.equals(UploadedFile.class)) {
			Form form = new FormImpl(httpRequest);
			// Form#getUploadedFile() throws IlegalArgumentException if form has not a single file upload part
			arguments[0] = form.getUploadedFile();
		}

		else if (type.equals(UploadStream.class)) {
			threadLocal.set((Closeable) (arguments[0] = getUploadStream(httpRequest, formalParameters)));
		}

		else if (type.equals(InputStream.class)) {
			threadLocal.set((Closeable) (arguments[0] = getUploadStream(httpRequest, formalParameters).openStream()));
		}

		else {
			arguments[0] = new FormObject(httpRequest, type).getValue();
		}

		return arguments;
	}

	/** Close stream from current thread storage then remove the storage. */
	@Override
	public void clean() {
		Files.close(threadLocal.get());
		threadLocal.remove();
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	/**
	 * Get upload stream from given HTTP request. This method expects on HTTP request a multipart form with a single part of
	 * byte stream type. Returns an upload stream instance wrapping the form stream part.
	 * 
	 * @param httpRequest HTTP request,
	 * @param formalParameters used only for exception message.
	 * @return upload stream.
	 * @throws IOException if reading form HTTP request fails.
	 * @throws IllegalArgumentException if there is no valid multipart form on HTTP request.
	 */
	private static UploadStream getUploadStream(HttpServletRequest httpRequest, Type[] formalParameters) throws IOException {
		FormIterator form = new FormIteratorImpl(httpRequest);
		if (!form.hasNext()) {
			throw new IllegalArgumentException("Empty form.");
		}
		Part part = form.next();
		if (!(part instanceof UploadStream)) {
			throw new IllegalArgumentException("Illegal form. Expected uploaded stream but got field |%s|.", part.getName());
		}
		return (UploadStream) part;
	}
}
