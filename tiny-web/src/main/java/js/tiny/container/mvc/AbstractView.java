package js.tiny.container.mvc;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import js.tiny.container.http.ContentType;
import js.tiny.container.http.HttpHeader;

/**
 * Views abstract base class. This base class initializes common HTTP response headers and delegates actual serialization to
 * concrete implementation, see {@link #serialize(OutputStream)}. Also implementation should provide view content type via
 * {@link #getContentType()}.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public abstract class AbstractView implements View {
	/**
	 * Get generated stream content type.
	 * 
	 * @return this view content type.
	 */
	protected abstract ContentType getContentType();

	/**
	 * Perform the actual content serialization to given output stream. Implementation has access to both view-meta and domain
	 * model instance that are both initialized before serialization execution.
	 * 
	 * @param outputStream output stream to serialize content to.
	 * @throws IOException if writing to output stream fails.
	 */
	protected abstract void serialize(OutputStream outputStream) throws IOException;

	/** View meta data loaded by view manager from external configuration object. */
	protected ViewMeta meta;

	/** Model instance used to populate this view instance. Model is supplied by application and can be null. */
	protected Object model;

	/**
	 * Set meta-data for this view instance.
	 * 
	 * @param meta view meat-data.
	 */
	void setMeta(ViewMeta meta) {
		this.meta = meta;
	}

	@Override
	public View setModel(Object model) {
		this.model = model;
		return this;
	}

	/**
	 * Set common HTTP response headers and delegates actual serialization to subclass. This method disables cache and set
	 * content type to value returned by subclass.
	 * 
	 * @param httpResponse HTTP response to serialize view to.
	 * @throws IOException if view serialization fails for whatever reasons.
	 */
	@Override
	public void serialize(HttpServletResponse httpResponse) throws IOException {
		httpResponse.setHeader(HttpHeader.CACHE_CONTROL, HttpHeader.NO_CACHE);
		httpResponse.addHeader(HttpHeader.CACHE_CONTROL, HttpHeader.NO_STORE);
		httpResponse.setHeader(HttpHeader.PRAGMA, HttpHeader.NO_CACHE);
		httpResponse.setDateHeader(HttpHeader.EXPIRES, 0);
		httpResponse.setContentType(getContentType().getValue());

		serialize(httpResponse.getOutputStream());
	}
}
