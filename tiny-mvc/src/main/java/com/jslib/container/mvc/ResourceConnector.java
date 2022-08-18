package com.jslib.container.mvc;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.container.http.Resource;
import com.jslib.container.mvc.annotation.Controller;
import com.jslib.container.spi.IClassPostLoadedProcessor;
import com.jslib.container.spi.IConnector;
import com.jslib.container.spi.IContainer;
import com.jslib.container.spi.IManagedClass;
import com.jslib.container.spi.IManagedMethod;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.jslib.util.Types;

/**
 * Container connector for MVC module.
 * 
 * @author Iulian Rotaru
 */
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

		log.debug("Scan MVC controller |{managed_class}|.", managedClass);
		for (IManagedMethod managedMethod : managedClass.getManagedMethods()) {
			if (managedMethod.isPublic() && Types.isKindOf(managedMethod.getReturnType(), Resource.class)) {
				String path = cache.add(managedMethod);
				log.debug("Register MVC method |{managed_method}| to path |{http_path}|.", managedMethod, path);
			}
		}
		return true;
	}
}
