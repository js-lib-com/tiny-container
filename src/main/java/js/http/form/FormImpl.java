package js.http.form;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import js.converter.ConverterRegistry;
import js.util.Files;
import js.util.Params;
import js.util.Strings;
import js.util.Types;

/**
 * Immutable implementation of {@link Form} interface.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public final class FormImpl implements Form {
	/** This form processed fields. */
	private final Map<String, FormField> fields = new HashMap<>();

	/** This form uploaded files. */
	private final Map<String, UploadedFile> uploadedFiles = new HashMap<>();

	/**
	 * Construct form instance and process all parts as arrive on HTTP request. If form contains streams they are saved on
	 * temporary files. Be aware that temporary file is removed at JVM exit.
	 * 
	 * @param httpRequest HTTP request carrying the multipart form.
	 * @throws IOException if HTTP request stream read fails.
	 */
	public FormImpl(HttpServletRequest httpRequest) throws IOException {
		FormIterator formIterator = new FormIteratorImpl(httpRequest);
		for (Part part : formIterator) {
			String name = part.getName();
			if (part instanceof UploadStream) {
				UploadStream us = (UploadStream) part;
				this.uploadedFiles.put(name, new UploadedFileImpl(name, us.getFileName(), us.getContentType(), Files.copy(us.openStream())));
			} else {
				FormField field = (FormField) part;
				this.fields.put(name, field);
			}
		}
	}

	@Override
	public boolean hasField(String name) {
		return getFormField(name) != null;
	}

	@Override
	public String getValue(String name) {
		FormField field = getFormField(name);
		return field != null ? field.getValue() : null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getValue(String name, Class<T> clazz) {
		FormField field = getFormField(name);
		return (T) (field != null ? ConverterRegistry.getConverter().asObject(field.getValue(), clazz) : Types.getEmptyValue(clazz));
	}

	@Override
	public boolean hasUpload(String name) {
		return getUploadedFile(name) != null;
	}

	@Override
	public UploadedFile getUploadedFile(String partName) {
		Params.notNullOrEmpty(partName, "Form part name");
		// key of the uploaded files map is normalized to Java member name
		return uploadedFiles.get(Strings.toMemberName(partName));
	}

	@Override
	public UploadedFile getUploadedFile() {
		if (!fields.isEmpty()) {
			throw new IllegalArgumentException("Attempt to retrieve unnamed file upload from form with fields.");
		}
		Iterator<UploadedFile> it = uploadedFiles.values().iterator();
		if (!it.hasNext()) {
			throw new IllegalArgumentException("Attempt to retrieve unnamed file upload from form with multiple uploads.");
		}
		return uploadedFiles.values().iterator().next();
	}

	@Override
	public File getFile(String name) {
		UploadedFile uploadedFile = getUploadedFile(name);
		return uploadedFile != null ? uploadedFile.getFile() : null;
	}

	/**
	 * Return named form field or null if there is none with requested name. Given name supports both dashed case or Java member
	 * name formats.
	 * 
	 * @param name form field name.
	 * @return named form field or null if not found.
	 */
	private FormField getFormField(String name) {
		// key of the form fields map is normalized to Java member name
		return fields.get(Strings.toMemberName(name));
	}
}
