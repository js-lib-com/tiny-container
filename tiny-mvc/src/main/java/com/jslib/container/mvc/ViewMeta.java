package com.jslib.container.mvc;

import java.io.File;
import java.util.Properties;

import com.jslib.util.Files;
import com.jslib.util.Strings;

/**
 * Immutable meta-data used for view instances creation and initialization. For that purpose meta-data keeps a reference to view
 * implementation class, see {@link #implementation}. Beside implementation, view meta-data has an unique name, template file
 * path and view specific properties.
 * 
 * @author Iulian Rotaru
 */
public final class ViewMeta {
	/**
	 * View name unique on application. It is publicly known to application and is used to retrieve view instances,
	 * {@link ViewManager#getView(String)}. By convention view name is the base name of attached template file.
	 */
	private final String name;

	/** View implementation class used to reflexively create view instances. */
	private final Class<? extends View> implementation;

	/** View template file. */
	private final File file;

	/**
	 * View class custom properties as described by <code>views</code> descriptor. These properties are common to all view
	 * instances of a given class.
	 * 
	 * <pre>
	 * &lt;repository path="..." files-pattern="*.fo" class="js.web.view.PdfView"&gt;
	 * 	&lt;property name="producer" value="j(s)-lib" /&gt;
	 * 	&lt;property name="creator" value="j(s)-lib" /&gt;
	 * &lt;/repository&gt;
	 * </pre>
	 */
	private final Properties properties;

	/** Cached value for instance string representation. */
	private final String string;

	/**
	 * Construct view meta instance.
	 * 
	 * @param file template file,
	 * @param implementation implementation class,
	 * @param properties optional view instance specific properties.
	 */
	public ViewMeta(File file, Class<? extends View> implementation, Properties properties) {
		this.name = Files.basename(file);
		this.file = file;
		this.implementation = implementation;
		this.properties = properties;
		this.string = Strings.concat(name, ':', implementation.getName(), ':', file);
	}

	/**
	 * Get view name, globally on application.
	 * 
	 * @return view name.
	 * @see #name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get view implementation class.
	 * 
	 * @return implementation class.
	 * @see #implementation
	 */
	public Class<? extends View> getImplementation() {
		return implementation;
	}

	/**
	 * Get view template file.
	 * 
	 * @return view template file.
	 * @see #file
	 */
	public File getTemplateFile() {
		return file;
	}

	/**
	 * Get named property value or null if property is not declared into <code>views</code> descriptor.
	 * 
	 * @param name property name.
	 * @return named property or null.
	 */
	public String getProperty(String name) {
		return properties.getProperty(name);
	}

	/**
	 * Test if view properties contains named property.
	 * 
	 * @param name property name.
	 * @return true if view properties contains named property.
	 */
	public boolean hasProperty(String name) {
		return properties.contains(name);
	}

	/**
	 * View meta instance string representation, mainly for debugging.
	 * 
	 * @return instance string representation.
	 */
	@Override
	public String toString() {
		return string;
	}
}
