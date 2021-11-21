package js.tiny.container.rest;

import javax.ejb.Remote;
import javax.inject.Inject;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.http.Resource;
import js.tiny.container.spi.IClassPostLoadedProcessor;
import js.tiny.container.spi.IConnector;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.util.Types;

public class RestConnector implements IConnector, IClassPostLoadedProcessor {
	private static final Log log = LogFactory.getLog(RestConnector.class);

	private final MethodsCache cache;

	@Inject
	public RestConnector() {
		log.trace("RestConnector()");
		this.cache = MethodsCache.instance();
	}

	public RestConnector(MethodsCache cache) {
		this.cache = cache;
	}

	@Override
	public Priority getPriority() {
		return Priority.SCAN;
	}

	@Override
	public <T> void onClassPostLoaded(IManagedClass<T> managedClass) {
		log.trace("onClassPostLoaded(IManagedClass<T>)");
		Remote remote = managedClass.getAnnotation(Remote.class);
		if (remote == null) {
			return;
		}

		log.debug("Scan REST controller |%s|.", managedClass);
		for (IManagedMethod managedMethod : managedClass.getManagedMethods()) {
			if (managedMethod.isPublic() && !Types.isKindOf(managedMethod.getReturnType(), Resource.class)) {
				String path = cache.add(managedMethod);
				log.debug("Register REST method |%s| to path |%s|.", managedMethod, path);
			}
		}
	}
}
