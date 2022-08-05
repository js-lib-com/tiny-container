package com.jslib.tiny.container.http.form;

import java.io.File;

import com.jslib.converter.Converter;
import com.jslib.converter.ConverterException;
import com.jslib.util.Types;

/**
 * Form with all parts parsed and loaded, including streams saved on temporary files. This form interface implementation has
 * intrinsic performance penalties due to its temporarily storage, critical for large files. In fact, limitation derived from
 * multipart format, the only solution being form parts sequential reading, including streams. In order to allow random access
 * there is no option but to temporarily store form part values.
 * <p>
 * {@link FormIterator} overcomes this drawback, allowing to application direct access to the form stream. Anyway, this
 * interface is handy when need random access to form items.
 * <p>
 * Form instance can be injected into resource or service methods. Anyway, it should be the only formal parameter from method
 * signature. Container takes care to create form instance and load if from HTTP request then provide as argument when invoke
 * managed method.
 * 
 * <pre>
 * &#064;Override
 * public void processForm(Form form) throws IOException {
 * 	// uses form getters to access fields and files
 * }
 * </pre>
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface Form {
	/**
	 * Returns true if form instance possess a field with requested name.
	 * 
	 * @param name the name of expected field.
	 * @return true if form has named field.
	 */
	boolean hasField(String name);

	/**
	 * Get string value for the named field or null if there is no field with requested name.
	 * 
	 * @param name the name of the form field.
	 * @return field string value possible null.
	 */
	String getValue(String name);

	/**
	 * Get named field value converted to desired type or default value if field not found. A value type is a primitive or a
	 * class that can be converted to a string - see {@link Converter}.
	 * 
	 * @param name the name of the form field,
	 * @param type desired value type.
	 * @param <T> value type.
	 * @return named field value or default value as defined by {@link Types#getEmptyValue(Type)}.
	 * @throws ConverterException if field value conversion fails.
	 */
	<T> T getValue(String name, Class<T> type) throws ConverterException;

	/**
	 * Returns true if form instance has requested named part, used for stream uploads.
	 * 
	 * @param name the name of expected upload part.
	 * @return true if form has named upload.
	 */
	boolean hasUpload(String name);

	/**
	 * Get uploaded file identified by named form part or null if named form part does not exists.
	 * 
	 * @param name the name of the form part denoting a file upload.
	 * @return uploaded file or null.
	 */
	UploadedFile getUploadedFile(String name);

	/**
	 * Convenient version of {@link #getUploadedFile(String)} for forms with a single file upload. In this case part name still
	 * exist into returned file upload and can be retrieved via {@link UploadedFile#getName()}.
	 * <p>
	 * To facilitate integration with user space service, like in sample code, this method throws illegal argument exception if
	 * this form has not a single part for type upload. This is to signal to remote client that form has parts that are not
	 * processed by service.
	 * 
	 * <pre>
	 * &#064;Override
	 * public void uploadForm(UploadedFile upload) throws IOException {
	 * 	upload.moveTo(getTargetDir());
	 * }
	 * </pre>
	 * 
	 * @return uploaded file.
	 * @throws IllegalArgumentException if form has has not a single part of type file upload.
	 */
	UploadedFile getUploadedFile();

	/**
	 * Convenient getter for uploaded temporary file. Returned temporary file should be treaded by application as it is:
	 * <code>temporary</code>. Note that it is removed on JVM exit.
	 * 
	 * @param name the name of the form part used to upload file.
	 * @return uploaded file.
	 */
	File getFile(String name);
}