package js.http.encoder;

import java.io.IOException;
import java.lang.reflect.Type;

import javax.servlet.http.HttpServletRequest;

import js.lang.SyntaxException;

/**
 * Reads invocation arguments from URL query parameters. This arguments reader implementation delegates
 * {@link QueryParametersParser} for URL parameters parsing.
 * 
 * @author Iulian Rotaru
 * @version final
 */
final class FormUrlArgumentsReader implements ArgumentsReader {
	/**
	 * Reads invocation arguments from HTTP request URL parameters accordingly formal parameters list. This arguments reader
	 * implementation delegates {@link QueryParametersParser} for URL parameters parsing.
	 * 
	 * @param httpRequest HTTP request,
	 * @param formalParameters formal parameters.
	 * @return arguments array.
	 * @throws IOException if reading from request input stream fails.
	 * @throws IllegalArgumentException if arguments from URL query parameters cannot be parsed.
	 */
	@Override
	public Object[] read(HttpServletRequest httpRequest, Type[] formalParameters) throws IOException, IllegalArgumentException {
		try {
			QueryParametersParser queryParameters = new QueryParametersParser(httpRequest.getInputStream());
			return queryParameters.getArguments(formalParameters);
		} catch (SyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/** This method does nothing but is requested by interface. */
	@Override
	public void clean() {
	}
}
