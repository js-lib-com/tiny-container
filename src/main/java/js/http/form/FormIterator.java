package js.http.form;

import java.io.IOException;
import java.util.Iterator;

/**
 * Multi-paradigm form iterator for on the fly parts processing. This interface takes the opposite approach compared with
 * {@link Form}. While Form implementation parses all form parts including streams saved on temporary files, FormIterator grants
 * access to streams on the fly which is effective, especially for large files.
 * <p>
 * This interface allows for form parts sequential access. If in need for random access one use {@link Form}.
 * <p>
 * Basically a form is an ordered collection of {@link Part} and this iterator is designed for this collection traversal. This
 * form iterator supports three paradigms: standard Java iterator, iterable and for each loop. This offers versatility but
 * overall performance is basically the same. Anyway <em>for each</em> loop tend to use less lines of code. In code samples
 * below we have a form <code>iterator</code> instance and an imaginary <code>person</code> domain object.
 * 
 * <pre>
 * while (iterator.hasNext()) {
 * 	Part part = iterator.next();
 * 	if (part instanceof UploadStream) {
 * 		UploadStream uploadStream = (UploadStream) part;
 * 		Files.copy(uploadStream.openStream());
 * 	} else {
 * 		FormField formField = (FormField) part;
 * 		Classes.setFieldValue(person, formField.getName(), formField.getValue());
 * 	}
 * }
 * </pre>
 * 
 * <pre>
 * for (Part part : iterator) {
 * 	if (part instanceof UploadStream) {
 * 		UploadStream uploadStream = (UploadStream) part;
 * 		Files.copy(uploadStream.openStream());
 * 	} else {
 * 		FormField formField = (FormField) part;
 * 		Classes.setFieldValue(person, formField.getName(), formField.getValue());
 * 	}
 * }
 * </pre>
 * 
 * <pre>
 * iterator.forEach(new FormHandler() {
 * 	&#064;Override
 * 	protected void stream(String name, String fileName, String contentType, InputStream inputStream) {
 * 		Files.copy(inputStream);
 * 	}
 * 
 * 	&#064;Override
 * 	protected void field(String name, String value) {
 * 		Classes.setFieldValue(person, name, value);
 * 	}
 * });
 * 
 * </pre>
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface FormIterator extends Iterator<Part>, Iterable<Part> {
	/**
	 * Parse multipart form in sequence and invoke specific part callback method on given handler.
	 * 
	 * @param handler user defined callback methods specific to form part.
	 * @throws IOException if form stream reading fails.
	 */
	void forEach(FormHandler handler) throws IOException;
}
