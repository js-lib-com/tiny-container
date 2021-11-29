package js.tiny.container.mvc;

import javax.inject.Inject;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.http.Resource;
import js.tiny.container.mvc.annotation.Controller;
import js.tiny.container.spi.IClassPostLoadedProcessor;
import js.tiny.container.spi.IConnector;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.util.Types;

public class ResourceConnector implements IConnector, IClassPostLoadedProcessor {
	private static final Log log = LogFactory.getLog(ResourceConnector.class);

	private final MethodsCache cache;

	@Inject
	public ResourceConnector() {
		log.trace("ResourceService()");
		this.cache = MethodsCache.instance();
	}

	/** Test constructor. */
	public ResourceConnector(MethodsCache cache) {
		this.cache = cache;
	}

	@Override
	public Priority getPriority() {
		return Priority.SCAN;
	}

	@Override
	public <T> void onClassPostLoaded(IManagedClass<T> managedClass) {
		log.trace("onClassPostLoaded(IManagedClass<T>)");
		Controller controller = managedClass.scanAnnotation(Controller.class);
		if (controller == null) {
			return;
		}

		log.debug("Scan MVC controller |%s|.", managedClass);
		for (IManagedMethod managedMethod : managedClass.getManagedMethods()) {
			if (managedMethod.isPublic() && Types.isKindOf(managedMethod.getReturnType(), Resource.class)) {
				String path = cache.add(managedMethod);
				log.debug("Register MVC method |%s| to path |%s|.", managedMethod, path);
			}
		}
	}
}
