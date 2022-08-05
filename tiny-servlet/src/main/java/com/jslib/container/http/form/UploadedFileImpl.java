package com.jslib.container.http.form;

import java.io.File;
import java.io.IOException;

import com.jslib.util.Files;
import com.jslib.util.Params;
import com.jslib.util.Strings;

/**
 * Immutable uploaded file implementation.
 * 
 * @author Iulian Rotaru
 * @version draft
 */
final class UploadedFileImpl implements UploadedFile {
	/** Part name. This value is guaranteed to be normalized to Java member like name. */
	private final String name;

	/** File name as described on remote host. */
	private final String fileName;

	/** Part content type. */
	private final String contentType;

	/** Uploaded temporary file. */
	private final File file;

	/** Instance string representation. */
	private final String string;

	/**
	 * Construct uploaded file instance. Given file is temporary and is removed at JVM exit.
	 * 
	 * @param name part name,
	 * @param fileName remote file name,
	 * @param contentType content type,
	 * @param file uploaded temporary file.
	 */
	UploadedFileImpl(String name, String fileName, String contentType, File file) {
		this.name = name;
		this.fileName = fileName;
		this.contentType = contentType;
		this.file = file;
		this.string = Strings.toString(this.name, this.fileName, this.contentType, this.file);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean is(String name) {
		Params.notNull(name, "Name");
		return name.equals(Strings.toMemberName(name));
	}

	@Override
	public String getFileName() {
		return fileName;
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public File getFile() {
		return file;
	}

	@Override
	public void moveTo(File targetDir) throws IOException {
		Params.isDirectory(targetDir, "Target directory");
		Files.renameTo(file, new File(targetDir, fileName));
	}

	/**
	 * Get instance string representation.
	 * 
	 * @return string representation.
	 */
	@Override
	public String toString() {
		return string;
	}
}