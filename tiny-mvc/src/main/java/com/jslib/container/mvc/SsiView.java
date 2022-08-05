package com.jslib.container.mvc;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.jslib.api.json.Json;
import com.jslib.util.Classes;
import com.jslib.util.Strings;

/**
 * HTML template based view using Server Side Content Injection. Template and content data serialized JSON are consolidated in a
 * single stream. On user agent site template is loaded into DOM and content data extracted from JSON and injected into DOM.
 * <p>
 * This technology is deprecated because it prevents caching, especially at user agent host.
 * 
 * @author Iulian Rotaru
 * @version draft
 * @deprecated
 */
final class SsiView extends AbstractView {
	private static final String SSI_CONTENT = "js.SSI-CONTENT";

	public void serialize(OutputStream outputStream) throws IOException {
		String html = Strings.load(meta.getTemplateFile());
		Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));

		try {
			int idx = html.lastIndexOf("</body>");
			writer.append(html.subSequence(0, idx));
			writer.append(String.format("\r\n<p id=\"%s\" style=\"display:none;visibility:hidden;\">", SSI_CONTENT));

			Json json = Classes.loadService(Json.class);
			json.stringify(new EscapeWriter(writer), model);

			writer.append("</p>\r\n");
			writer.append(html.substring(idx));
		} finally {
			writer.flush();
			writer.close();
		}
	}

	/**
	 * XML escape write.
	 * 
	 * @author Iulian Rotaru
	 * @version draft
	 * @deprecated
	 */
	private static class EscapeWriter extends Writer {
		private Writer writer;

		public EscapeWriter(Writer writer) {
			this.writer = writer;
		}

		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			for (int i = off, l = off + len; i < l; ++i) {
				char c = cbuf[i];
				switch (c) {
				case '"':
					writer.write("&quot;");
					break;
				case '\'':
					writer.write("&apos;");
					break;
				case '&':
					writer.write("&amp;");
					break;
				case '<':
					writer.write("&lt;");
					break;
				case '>':
					writer.write("&gt;");
					break;
				default:
					writer.write(c);
				}
			}
		}

		@Override
		public void flush() throws IOException {
			writer.flush();
		}

		@Override
		public void close() throws IOException {
			// do not close underlying writer
		}
	}
}
