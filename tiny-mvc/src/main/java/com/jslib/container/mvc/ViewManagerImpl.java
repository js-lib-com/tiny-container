package com.jslib.container.mvc;

import java.io.IOException;
import java.util.Properties;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.container.servlet.RequestContext;
import com.jslib.container.spi.IContainer;
import com.jslib.lang.BugError;
import com.jslib.lang.Config;
import com.jslib.lang.ConfigBuilder;
import com.jslib.lang.ConfigException;
import com.jslib.util.Classes;
import com.jslib.util.I18nFile;
import com.jslib.util.I18nPool;
import com.jslib.util.I18nRepository;
import com.jslib.util.Types;

/**
 * View manager implementation. At its core views manager has a pool of {@link ViewMeta} instances, one for each application
 * view. Views meta pool is populated from configuration object that describes templates repositories. Using meta data, view
 * manager creates and initializes view instances, see {@link #getView(String)}.
 * <p>
 * Views repositories configuration. Views configuration is stored into <code>views</code> section from application descriptor.
 * There is a child element for every repository. A repository is stored in a directory and has optional files pattern. To
 * select all files uses '*' wild card.
 * 
 * <pre>
 * &lt;views&gt;
 * 	&lt;repository path="/var/www/vhosts/site/${language}" files-pattern="*.html" class="js.web.view.XspView" /&gt;
 * 	&lt;repository path="/var/www/vhosts/site/${language}/fo" files-pattern="*.fo" class="js.web.view.PdfView"&gt;
 * 		&lt;property name="producer" value="Phantom Co." /&gt;
 * 		&lt;property name="creator" value="John Doe" /&gt;
 * 	&lt;/repository&gt;
 * &lt;/views&gt;
 * </pre>
 * 
 * @author Iulian Rotaru
 */
public final class ViewManagerImpl implements ViewManager {
	private static final Log log = LogFactory.getLog(ViewManager.class);

	/** Default view implementation is {@link XspView}. */
	private static final String DEF_IMPLEMENTATION = XspView.class.getName();

	private final IContainer container;
	
	/**
	 * Application global repository for views meta. Because all application views meta are stored in the same repository, view
	 * name should be unique across application. For example, a HTML view cannot have the same name with a PDF view, even
	 * templates are stored into different directories.
	 */
	private I18nPool<ViewMeta> viewsMetaPool;

	public ViewManagerImpl(IContainer container) {
		this.container = container;
	}

	/**
	 * Create views meta pool from given managed view configuration object. Configuration object is described on class
	 * description.
	 * 
	 * @param config view manager configuration object.
	 * @throws ConfigException if configuration object is not valid.
	 * @throws IOException if views meta pool cannot be created.
	 */
	public void config(Config config) throws ConfigException, IOException {
		for (Config repositorySection : config.findChildren("repository")) {
			// view manager configuration section is named <views>
			// a <views> configuration section has one or many <repository> child sections
			// scan every repository files accordingly files pattern and add meta views to views meta pool

			// load repository view implementation class and perform insanity checks
			String className = repositorySection.getAttribute("class", DEF_IMPLEMENTATION);
			Class<?> implementation = Classes.forOptionalName(className);
			if (implementation == null) {
				throw new ConfigException("Unable to load view implementation |%s|.", className);
			}
			if (!Types.isKindOf(implementation, View.class)) {
				throw new ConfigException("View implementation |%s| is not of proper type.", className);
			}
			if (!Classes.isInstantiable(implementation)) {
				throw new ConfigException("View implementation |%s| is not instantiable. Ensure is not abstract or interface and have default constructor.", implementation);
			}
			@SuppressWarnings("unchecked")
			Class<? extends View> viewImplementation = (Class<? extends View>) implementation;

			// load repository path and files pattern and create I18N repository instance
			String repositoryPath = repositorySection.getAttribute("path");
			if (repositoryPath == null) {
				throw new ConfigException("Invalid views repository configuration. Missing <path> attribute.");
			}
			String filesPattern = repositorySection.getAttribute("files-pattern");
			if (filesPattern == null) {
				throw new ConfigException("Invalid views repository configuration. Missing <files-pattern> attribute.");
			}

			ConfigBuilder builder = new I18nRepository.ConfigBuilder(repositoryPath, filesPattern);
			I18nRepository repository = new I18nRepository(builder.build());
			if (viewsMetaPool == null) {
				// uses first repository to initialize i18n pool
				// limitation for this solution is that all repositories should be the kind: locale sensitive or not
				viewsMetaPool = repository.getPoolInstance();
			}
			Properties properties = repositorySection.getProperties();

			// traverses all files from I18N repository instance and register view meta instance
			// builder is used by view meta to load the document template
			for (I18nFile template : repository) {
				ViewMeta meta = new ViewMeta(template.getFile(), viewImplementation, properties);
				if (viewsMetaPool.put(meta.getName(), meta, template.getLocale())) {
					log.warn("Override view |{view_meta}|", meta);
				} else {
					log.debug("Register view |{view_meta}|", meta);
				}
			}
		}
	}

	/**
	 * Create view instance based on view meta data identified by name and request locale. View instance is created from view
	 * implementation class - {@link ViewMeta#getImplementation()}.
	 * 
	 * @param viewName application unique view name.
	 * @return newly create view instance.
	 * @throws BugError if given view name does not designate an existing view.
	 */
	@Override
	public View getView(String viewName) {
		RequestContext context = container.getInstance(RequestContext.class);
		ViewMeta meta = viewsMetaPool.get(viewName, context.getLocale());
		if (meta == null) {
			throw new BugError("View |%s| not found. View name may be misspelled, forgot to add template file or template name doesn't match views files pattern.", viewName);
		}
		AbstractView view = (AbstractView) Classes.newInstance(meta.getImplementation());
		view.setMeta(meta);
		return view;
	}
}
