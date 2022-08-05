package com.jslib.tiny.container.http.form;

import java.io.IOException;
import java.io.InputStream;

/**
 * Live upload stream is a form part encoded <code>application/octet-stream</code> or the like. It grants access to form stream
 * for on the fly processing.
 * <p>
 * Standard usage pattern is to traverse form parts using {@link FormIterator}, check instance of and cast, like in sample code.
 * 
 * <pre>
 * for(Part part : form) {
 *   if(part instanceof UploadStream) {
 *     UploadStream uploadStream = (UploadStream)part;
 *     Files.copy(uploadStream.openStream());
 *   }
 *   ...
 * }
 * </pre>
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface UploadStream extends Part {
	/**
	 * File name as designated on remote host. If remote file name contains path directories, returns only file name with
	 * extension preserved.
	 * 
	 * @return remote file name.
	 */
	String getFileName();

	/**
	 * Open this form part input stream for on the fly processing. Please note it is caller responsibility to close this stream.
	 * 
	 * @return this form part input stream.
	 * @throws IOException stream opening operation fails.
	 */
	InputStream openStream() throws IOException;
}