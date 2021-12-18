package js.tiny.container.rest;

import java.util.List;

import javax.inject.Inject;

import js.json.Json;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.http.Resource;
import js.tiny.container.spi.IClassPostLoadedProcessor;
import js.tiny.container.spi.IConnector;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.util.Types;

public class RestConnector implements IConnector, IClassPostLoadedProcessor {
	private static final Log log = LogFactory.getLog(RestConnector.class);

	private final MethodsCache cache;

	@Inject
	public RestConnector() {
		this.cache = MethodsCache.instance();
	}

	/** Test constructor. */
	public RestConnector(MethodsCache cache) {
		this.cache = cache;
	}

	@Override
	public void create(IContainer container) {
		container.bind(Json.class).service().build();
	}

	@Override
	public Priority getPriority() {
		return Priority.SCAN;
	}

	@Override
	public <T> boolean onClassPostLoaded(IManagedClass<T> managedClass) {
		if (IRemote.scan(managedClass) == null) {
			return false;
		}

		log.debug("Scan REST controller |%s|.", managedClass);
		for (IManagedMethod managedMethod : managedClass.getManagedMethods()) {
			if (managedMethod.isPublic() && !Types.isKindOf(managedMethod.getReturnType(), Resource.class)) {
				List<String> paths = cache.add(managedMethod);
				log.debug("Register REST method |%s| to path |%s|.", managedMethod, paths);
			}
		}
		return true;
	}
}
