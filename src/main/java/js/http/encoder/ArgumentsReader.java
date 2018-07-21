package js.http.encoder;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

import javax.servlet.http.HttpServletRequest;

/**
 * Method invocation arguments reader from HTTP request. Arguments reader deserializes a net method invocation arguments carried
 * by HTTP request, accordingly formal parameter types. Arguments are not necessarily encoded into request body. Implementation
 * may alternatively use URL query parameters. Also implementation may need extra information from request headers. This is the
 * reason why {@link #read(HttpServletRequest, Type[])} method signature uses {@link HttpServletRequest} and not
 * {@link InputStream}.
 * <p>
 * Arguments reader may have internal state that should be updated when arguments processing is done. Optional method
 * {@link #clean()} is used to signal arguments processing done. It can be used to release reader state; anyway, since reader
 * implementation is reused its internal state should be stored on and released from thread local storage.
 * <p>
 * Implementation should not close HTTP request input stream. This is performed by outer logic.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface ArgumentsReader {
	/**
	 * Read arguments from HTTP request accordingly given formal parameter types.
	 * 
	 * @param httpRequest HTTP request,
	 * @param formalParameters formal parameter types.
	 * @return arguments array.
	 * @throws IOException if reading from request input stream fails.
	 * @throws IllegalArgumentException if arguments from HTTP request cannot be parsed.
	 */
	Object[] read(HttpServletRequest httpRequest, Type[] formalParameters) throws IOException, IllegalArgumentException;

	/**
	 * Optional arguments reading cleanup. This method may be used to clean reader state stored on thread local storage. It is
	 * not expected that this method to perform IO operation and does not throws IO related exceptions. Especially
	 * implementation should not use this method to close HTTP request input stream. Closing HTTP request is performed by outer
	 * logic.
	 */
	void clean();
}
