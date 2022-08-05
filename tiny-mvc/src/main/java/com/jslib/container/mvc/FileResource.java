package com.jslib.container.mvc;

import java.io.File;
import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import com.jslib.container.http.ContentType;
import com.jslib.container.http.HttpHeader;
import com.jslib.container.http.Resource;
import com.jslib.util.Files;
import com.jslib.util.Params;

/**
 * Resource implementation used to send files back to the client. It takes care to initialize HTTP response headers and
 * serialize wrapped file to HTTP response output stream. Current implementation disable cache.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public final class FileResource implements Resource {
	/** Resource file path. */
	private final File file;
	/** Resource content type. */
	private final String contentType;

	/**
	 * Create file resource for requested file path and content type inferred from extension. Uses
	 * {@link ContentType#forFile(File)} to infer content type.
	 * 
	 * @param path file path, absolute or relative to current working directory.
	 * @throws IllegalArgumentException if <code>path</code> argument is null or empty or does not denote an ordinary file.
	 */
	public FileResource(String path) {
		this(file(path));
	}

	/**
	 * Create file resource for requested file and content type inferred from extension. Uses {@link ContentType#forFile(File)}
	 * to infer content type.
	 * 
	 * @param file source file, absolute or relative to current working directory.
	 * @throws IllegalArgumentException if <code>file</code> argument is null or not an ordinary file.
	 */
	public FileResource(File file) {
		this(file, ContentType.forFile(file));
	}

	/**
	 * Create file resource for requested file path and content type.
	 * 
	 * @param path file path, absolute or relative to current working directory.
	 * @param contentType file content type.
	 * @throws IllegalArgumentException if <code>path</code> argument is null or empty or does not denote an ordinary file.
	 * @throws IllegalArgumentException if <code>contentType</code> argument is null.
	 */
	public FileResource(String path, ContentType contentType) {
		this(file(path), contentType);
	}

	/**
	 * Create file resource for requested file and content type.
	 * 
	 * @param file source file, absolute or relative to current working directory.
	 * @param contentType file content type.
	 * @throws IllegalArgumentException if <code>file</code> argument is null or not an ordinary file.
	 * @throws IllegalArgumentException if <code>contentType</code> argument is null.
	 */
	public FileResource(File file, ContentType contentType) {
		Params.notNull(file, "File");
		Params.isFile(file, "File");
		Params.notNull(contentType, "Content type");
		this.file = file;
		this.contentType = contentType.getValue();
	}

	/**
	 * Create file for given path.
	 * 
	 * @param path file path.
	 * @return newly create file.
	 * @throws IllegalArgumentException if <code>path</code> argument is null or empty.
	 */
	private static File file(String path) {
		Params.notNullOrEmpty(path, "File path");
		return new File(path);
	}

	/**
	 * Serialize this file resource to HTTP response. Disable cache and set content type and content length.
	 * 
	 * @param httpResponse HTTP response.
	 * @throws IOException if writing to underlying stream fails.
	 */
	@Override
	public void serialize(HttpServletResponse httpResponse) throws IOException {
		httpResponse.setHeader(HttpHeader.CACHE_CONTROL, HttpHeader.NO_CACHE);
		httpResponse.addHeader(HttpHeader.CACHE_CONTROL, HttpHeader.NO_STORE);
		httpResponse.setHeader(HttpHeader.PRAGMA, HttpHeader.NO_CACHE);
		httpResponse.setDateHeader(HttpHeader.EXPIRES, 0);
		httpResponse.setContentType(contentType);
		httpResponse.setHeader(HttpHeader.CONTENT_LENGTH, Long.toString(file.length()));
		Files.copy(file, httpResponse.getOutputStream());
	}
}
