package com.jslib.container.mvc;

import com.jslib.container.http.ContentType;
import com.jslib.container.http.Resource;

/**
 * Views are resources specialized on model objects presentation. A view instance serializes model on given HTTP response
 * stream, in a format client knows to interpret and takes care to properly set <code>Content-Type</code>. Most common used
 * content type is {@link ContentType#TEXT_HTML} but by no means is limited to. For example, there are plugins that provides
 * views for PDF and CSV documents.
 * <p>
 * ASCII diagram below tries to catch view core abstractions: view serializes model to stream with content type.
 * 
 * <pre>
 * DATA-SOURCE -- MODEL -- VIEW -- STREAM / CONTENT-TYPE
 * </pre>
 * <p>
 * Views are generally returned by controller methods, see sample code.
 * 
 * <pre>
 * &#064;Controller
 * class PageFlow {
 * 	ViewManager viewManager;
 * 	Dao dao;
 * 
 * 	View contact(String name) {
 * 		Contact contact = dao.get(Contact.class, &quot;from Contact where name=?&quot;, name);
 * 		return viewManager.getView(&quot;contact-view&quot;).setModel(contact);
 * 	}
 * }
 * </pre>
 * <p>
 * User defined views are supported but implementation should take care of entire serialization process. Also should not forget
 * to set <code>Content-Type</code> otherwise client may not be able to properly render the stream. In sample code below a hello
 * world view is implementing {@link Resource} interface but one may choose this interface or even to extend
 * {@link AbstractView} class.
 * 
 * <pre>
 * public class HelloWorldView implements Resource {
 * 	public void serialize(HttpServletResponse res) throws Exception {
 * 		res.setContentType(&quot;text/html;charset=UTF-8&quot;);
 * 
 * 		Writer w = res.getWriter();
 * 		w.println(&quot;&lt;!DOCTYPE html&gt;&quot;);
 * 		w.println(&quot;&lt;html&gt;&quot;);
 * 		w.println(&quot; &lt;head&gt;&quot;);
 * 		w.println(&quot;     &lt;meta http-equiv='Content-Type' content='text/html;charset=UTF-8' /&gt;&quot;);
 * 		w.println(&quot; &lt;/head&gt;&quot;);
 * 		w.println(&quot; &lt;body&gt;&quot;);
 * 		w.println(&quot;     &lt;h1&gt;Hello World!&lt;/h1&gt;&quot;);
 * 		w.println(&quot; &lt;/body&gt;&quot;);
 * 		w.println(&quot;&lt;/html&gt;&quot;);
 * 	}
 * }
 * </pre>
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface View extends Resource {
	/**
	 * Set domain model that this view is representing. Model object can be any plain Java containing fields needed by this
	 * view. It is provided by application and used by this view to generate dynamic content. View static part is provided by
	 * template.
	 * 
	 * @param model instance of domain model provided by application.
	 * @return this pointer.
	 */
	View setModel(Object model);
}
