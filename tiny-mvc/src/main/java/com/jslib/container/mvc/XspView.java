package com.jslib.container.mvc;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.jslib.api.template.Template;
import com.jslib.api.template.TemplateEngine;
import com.jslib.util.Classes;

/**
 * X(HT)ML Server Page - server generated pages from X(HT)ML templates. This class implements a {@link View} based on (X)HTML
 * {@link Template} processed on server side. In essence this class serialize back to client a (X)HTML page template with model
 * injected.
 * <p>
 * XSP view uses {@link TemplateEngine} to inject model and serialize view to HTTP response. It is developer and deployer
 * responsibility to ensure that view templates syntax is compatible with template engine implementation. Template engine is a
 * Java service and its implementation is loaded at run-time.
 * <p>
 * XSP view should be declared into <code>views</code> section from application descriptor. In sample configuration it is also
 * enabled operators serialization. By default operators serialization is disabled.
 * 
 * <pre>
 * &lt;views&gt;
 * 	&lt;repository path='templates/path' files-pattern='*.htm'&gt;
 * 		&lt;property name='template.operator.serialization' value='true' /&gt;
 * 	&lt;/repository&gt; 
 * &lt;/views&gt;
 * </pre>
 * 
 * @author Iulian Rotaru
 */
public final class XspView extends AbstractView {
	/** Key for operator serialization property. */
	private static final String OPERATOR_SERIALIZATION = "template.operator.serialization";

	/**
	 * Uses template engine to inject model and serialize view on HTTP response. If meta-view contains
	 * {@link #OPERATOR_SERIALIZATION} property enable engine operators serialization.
	 * 
	 * @param outputStream output stream of the HTTP response.
	 * @throws IOException if stream write operation fails.
	 */
	@Override
	protected void serialize(OutputStream outputStream) throws IOException {
		// refrain to use HttpResponse#getWriter()
		// this library always uses output stream since servlet response API does not allow mixing characters and bytes streams
		Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));

		TemplateEngine templateEngine = Classes.loadService(TemplateEngine.class);
		Template template = templateEngine.getTemplate(meta.getTemplateFile());
		boolean operatorSerialization = Boolean.parseBoolean(meta.getProperty(OPERATOR_SERIALIZATION));
		if (operatorSerialization) {
			template.setProperty("com.jslib.api.template.serialize.operator", true);
		}
		// if model is null template serializes itself skipping injection operation
		template.serialize(model, writer);

		// there is no need to flush response stream on error so no need for try/finally
		writer.flush();
	}
}
