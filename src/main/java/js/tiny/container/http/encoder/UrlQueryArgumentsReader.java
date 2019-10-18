package js.tiny.container.http.encoder;

import java.io.IOException;
import java.lang.reflect.Type;

import javax.servlet.http.HttpServletRequest;

import js.lang.SyntaxException;

/**
 * Invocation arguments reader for URL query parameters. This arguments reader just delegates {@link QueryParametersParser}.
 * 
 * @author Iulian Rotaru
 * @version final
 */
final class UrlQueryArgumentsReader implements ArgumentsReader {
	/**
	 * Read arguments from URL query parameters.
	 * 
	 * @param httpRequest HTTP request,
	 * @param formalParameters formal parameter types.
	 * @return arguments array.
	 * @throws IOException if reading from request input stream fails.
	 * @throws IllegalArgumentException if arguments from HTTP request cannot be parsed.
	 */
	@Override
	public Object[] read(HttpServletRequest httpRequest, Type[] formalParameters) throws IOException, IllegalArgumentException {
		try {
			QueryParametersParser queryParameters = new QueryParametersParser(httpRequest.getQueryString());
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
