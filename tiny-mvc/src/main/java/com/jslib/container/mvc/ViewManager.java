package com.jslib.container.mvc;

/**
 * View manager creates named views. Views handled by this manager have a name unique per application.
 * 
 * @author Iulian Rotaru
 */
public interface ViewManager {
	/**
	 * Return an instance of the view identified by view name. Implementation is free to create new instances or to reuse from
	 * caches.
	 * 
	 * @param viewName view name, unique per application.
	 * @return requested view instance.
	 */
	View getView(String viewName);
}
