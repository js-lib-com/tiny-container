package js.tiny.container.mvc;

import javax.inject.Inject;

import jakarta.inject.Singleton;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.http.Resource;
import js.tiny.container.mvc.annotation.Controller;
import js.tiny.container.spi.IClassPostLoadedProcessor;
import js.tiny.container.spi.IConnector;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.util.Types;

public class ResourceConnector implements IConnector, IClassPostLoadedProcessor {
	private static final Log log = LogFactory.getLog(ResourceConnector.class);

	private MethodsCache cache;

	@Inject
	public ResourceConnector() {
		log.trace("ResourceService()");
	}

	/** Test constructor. */
	public ResourceConnector(MethodsCache cache) {
		this.cache = cache;
	}

	@Override
	public void configure(IContainer container) {
		container.bind(MethodsCache.class).in(Singleton.class).build();
	}

	@Override
	public void create(IContainer container) {
		cache = container.getInstance(MethodsCache.class);
	}

	@Override
	public <T> boolean bind(IManagedClass<T> managedClass) {
		return managedClass.scanAnnotation(Controller.class) != null;
	}

	@Override
	public Priority getPriority() {
		return Priority.SCAN;
	}

	@Override
	public <T> boolean onClassPostLoaded(IManagedClass<T> managedClass) {
		Controller controller = managedClass.scanAnnotation(Controller.class);
		if (controller == null) {
			return false;
		}

		log.debug("Scan MVC controller |%s|.", managedClass);
		for (IManagedMethod managedMethod : managedClass.getManagedMethods()) {
			if (managedMethod.isPublic() && Types.isKindOf(managedMethod.getReturnType(), Resource.class)) {
				String path = cache.add(managedMethod);
				log.debug("Register MVC method |%s| to path |%s|.", managedMethod, path);
			}
		}
		return true;
	}
}
