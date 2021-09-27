package js.tiny.container.http.encoder;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

/**
 * Single argument reader from multipart mixed message. Mutipart mixed format is a collection of body entities in the same
 * message. This reader is enacted for every entity body from multipart mixed message. It gets an input stream that spans only
 * on entity body part.
 * <p>
 * Implementation should not close body entity input stream. This is performed by {@link MultipartMixedArgumentsReader} logic.
 * <p>
 * Implementation note: Argument part reader is a specialization of method invocation arguments reader. This is not so because
 * of logic dependencies: argument part reader does not use method invocation arguments reader services. But is for
 * implementation optimization since it is expected that both implementations to share the common logic.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface ArgumentPartReader extends ArgumentsReader {
	/**
	 * Read argument from body entity input stream. Input stream argument covers only body entity part, not entire message. So
	 * this method can read it till EOF. Anyway, is not necessary to close the stream since outer logic takes care of it.
	 * 
	 * @param inputStream body entity input stream,
	 * @param parameterType expected parameter type.
	 * @return deserialized argument.
	 * @throws IOException if reading from body entity input stream fails.
	 */
	Object read(InputStream inputStream, Type parameterType) throws IOException;
}
