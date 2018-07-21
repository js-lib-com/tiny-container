package js.http.form;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import js.converter.ConverterRegistry;
import js.log.Log;
import js.log.LogFactory;
import js.util.Files;
import js.util.Params;
import js.util.Strings;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 * Multipart form iterator implementation. This class implements both {@link FormIterator} direct interface and inherited ones,
 * {@link Iterator} and {@link Iterable}. This implementation has side effects: it takes care to close form parts while
 * traversing form iterator so that application does not need to.
 * <p>
 * Although form iterator implementation is public it is designed for framework internal usage; it helps injecting
 * {@link FormIterator} on resource or service methods. Container uses this class to create and initialize FormIterator then
 * inject it as managed method argument.
 * 
 * <pre>
 * &#064;Override
 * public void saveCustomer(FormIterator form) {
 * 	for (Part part : form) {
 * 		// handle form parts one by one, in sequence
 * 		...
 * 		// no need to close the form part
 * 	}
 * }
 * </pre>
 * 
 * @author Iulian Rotaru
 * @version final
 */
public final class FormIteratorImpl implements FormIterator {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(FormIterator.class);

	/** File item iterator. */
	private FileItemIterator fileItemIterator;

	/** Form currently processing part. */
	private PartImpl currentPart;

	/**
	 * Construct form iterator with data from given HTTP request. HTTP request Content-Type type should be
	 * <code>multipart</code> and <code>boundary</code> parameter should be defined.
	 * 
	 * @param httpRequest HTTP request carrying the multipart form.
	 * @throws IOException if HTTP request read operation fails, content type has no boundary or form body parsing fails.
	 */
	public FormIteratorImpl(HttpServletRequest httpRequest) throws IOException {
		ServletFileUpload upload = new ServletFileUpload();
		try {
			this.fileItemIterator = upload.getItemIterator(httpRequest);
		} catch (FileUploadException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Test constructor.
	 * 
	 * @param fileItemIterator mock for underlying file item iterator.
	 */
	public FormIteratorImpl(FileItemIterator fileItemIterator) {
		this.fileItemIterator = fileItemIterator;
	}

	@Override
	public void forEach(FormHandler handler) throws IOException {
		for (Part part : this) {
			if (part instanceof UploadStream) {
				UploadStreamImpl uploadStream = (UploadStreamImpl) part;
				InputStream inputStream = uploadStream.openStream();
				try {
					handler.stream(uploadStream.getName(), uploadStream.getFileName(), uploadStream.getContentType(), inputStream);
				} catch (IOException e) {
					throw e;
				} catch (Throwable t) {
					throw new RuntimeException(t);
				} finally {
					Files.close(uploadStream);
				}
			} else {
				FormField formField = (FormField) part;
				try {
					handler.field(formField.getName(), formField.getValue());
				} catch (Throwable t) {
					throw new RuntimeException(t);
				}
			}
		}
	}

	/**
	 * Return form parts iterator.
	 * 
	 * @return this form parts iterator.
	 */
	@Override
	public Iterator<Part> iterator() {
		return this;
	}

	/**
	 * Test if there are more parts on this form iterator. This implementation has side effects: it takes care to close form
	 * parts while traversing form iterator.
	 * 
	 * @return true if there is at least one available form part.
	 */
	@Override
	public boolean hasNext() {
		try {
			if (currentPart != null) {
				currentPart.close();
			}
			if (!fileItemIterator.hasNext()) {
				return false;
			}
			FileItemStream fileItemStream = fileItemIterator.next();
			if (fileItemStream.isFormField()) {
				currentPart = new FormFieldImpl(fileItemStream);
			} else {
				currentPart = new UploadStreamImpl(fileItemStream);
			}
			return true;
		} catch (IOException | FileUploadException e) {
			log.error(e);
		}
		return false;
	}

	/**
	 * Get next part from this form iterator. This method should be called only if {@link #hasNext()} returns true. Otherwise
	 * behavior is not defined.
	 * 
	 * @return form part, never null if {@link #hasNext()} was true.
	 */
	@Override
	public Part next() {
		return currentPart;
	}

	/** Unsupported operation. */
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	// --------------------------------------------------------------------------------------------
	// INNER CLASSES

	/**
	 * Form part implementation.
	 * 
	 * @author Iulian Rotaru
	 * @version final
	 */
	private abstract class PartImpl implements Part, Closeable {
		/** File item stream that facilitates access to form part stream. */
		protected final FileItemStream fileItemStream;
		/** Part name, normalized to Java member like syntax. */
		private final String name;

		public PartImpl(FileItemStream fileItemStream) {
			this.fileItemStream = fileItemStream;
			this.name = Strings.toMemberName(this.fileItemStream.getFieldName());
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
		public String getContentType() {
			return fileItemStream.getContentType();
		}
	}

	/**
	 * Form field implementation.
	 * 
	 * @author Iulian Rotaru
	 * @version final
	 */
	private class FormFieldImpl extends PartImpl implements FormField {
		/** Form field string value. */
		private final String value;

		/**
		 * Create form filed instance and load its value from underlying form. Closes file item stream after loading this form
		 * field value.
		 * 
		 * @param fileItemStream file item stream that facilitates access to form part stream.
		 * @throws IOException if fail to read form part stream.
		 */
		public FormFieldImpl(FileItemStream fileItemStream) throws IOException {
			super(fileItemStream);
			this.value = Strings.load(fileItemStream.openStream());
		}

		@Override
		public String getValue() {
			return value;
		}

		@Override
		public <T> T getValue(Class<T> type) {
			return ConverterRegistry.getConverter().asObject(getValue(), type);
		}

		/** This method does nothing since file item stream is closed by constructor after value loading. */
		@Override
		public void close() {
		}
	}

	/**
	 * Upload stream implementation.
	 * 
	 * @author Iulian Rotaru
	 * @version final
	 */
	private class UploadStreamImpl extends PartImpl implements UploadStream {
		/** Form upload input stream. */
		private InputStream inputStream;

		/**
		 * Create form upload stream instance wrapping underlying form part stream.
		 * 
		 * @param fileItemStream file item stream that facilitates access to form part stream.
		 * @throws IOException if fail to read form part stream.
		 */
		public UploadStreamImpl(FileItemStream fileItemStream) throws IOException {
			super(fileItemStream);
		}

		@Override
		public String getFileName() {
			String fileName = fileItemStream.getName();
			int i = fileName.lastIndexOf('/');
			if (i == -1) {
				i = fileName.lastIndexOf('\\');
			}
			return i != -1 ? fileName.substring(i + 1) : fileName;
		}

		@Override
		public InputStream openStream() throws IOException {
			inputStream = fileItemStream.openStream();
			return inputStream;
		}

		/**
		 * Close underlying input stream, if opened by {@link #openStream()}. Otherwise this method does nothing.
		 * 
		 * @throws IOException if input stream close operation fails.
		 */
		@Override
		public void close() throws IOException {
			if (inputStream != null) {
				inputStream.close();
			}
		}
	}
}
