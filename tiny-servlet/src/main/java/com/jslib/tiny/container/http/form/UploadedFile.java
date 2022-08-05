package com.jslib.tiny.container.http.form;

import java.io.File;
import java.io.IOException;

/**
 * Temporary stored uploaded file. This is a file processed by {@link Form} implementation. Form stream is completely parsed and
 * copied in a temporary file. User space code can use {@link #moveTo(File)} to effectively move the file in the desired place -
 * this move operation is to avoid multiple copy.
 * <p>
 * Usually uploaded file is retrieved using {@link Form#getUploadedFile(String)}, see sample code. Target directory should exist
 * and rights correctly set.
 * 
 * <pre>
 * &#064;Override
 * public void uploadForm(Form form) throws IOException {
 * 	UploadedFile upload = form.getUploadedFile(&quot;file&quot;);
 * 	upload.moveTo(getTargetDir());
 * }
 * </pre>
 * <p>
 * Anyway, if client form contains only a single file upload, user space service can use this interface directly. Framework
 * takes care to process the form and to create uploaded file instance.
 * 
 * <pre>
 * &#064;Override
 * public void uploadForm(UploadedFile upload) throws IOException {
 * 	upload.moveTo(getTargetDir());
 * }
 * </pre>
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface UploadedFile extends Part {
	/**
	 * File name as designated on remote host.
	 * 
	 * @return remote file name.
	 */
	String getFileName();

	/**
	 * Get uploaded stream content type.
	 * 
	 * @return uploaded stream content type.
	 */
	String getContentType();

	/**
	 * Get underlying temporary file created for this upload. Be aware that this temporary file is removed at JVM exit.
	 * 
	 * @return underlying temporary file.
	 */
	File getFile();

	/**
	 * Move this uploaded file in specified target directory using remote file name. Target directory should exist and
	 * authorization correctly set. If target file already exists it is silently overwritten.
	 * <p>
	 * When {@link Form} implementation processes uploaded stream it creates a file in a temporarily storage. In order to avoid
	 * a second copy one can use this method to just move already created file in the right place.
	 * <p>
	 * This method is just a convenient way to move in a known target directory while keeping original file name. If ones
	 * desires another name can use {@link File#renameTo(File)} or {@link com.jslib.util.Files#renameTo(File, File)}.
	 * 
	 * @param targetDir target directory to move uploaded file to.
	 * @throws IOException if file move operations fails for platform dependent causes, e.g. operation not authorized for
	 *             current process.
	 * @throws IllegalArgumentException if target directory argument is null or does not denote an existing directory.
	 */
	void moveTo(File targetDir) throws IOException;
}
