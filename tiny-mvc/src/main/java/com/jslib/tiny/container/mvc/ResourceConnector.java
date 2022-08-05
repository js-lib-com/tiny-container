package com.jslib.tiny.container.mvc;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.tiny.container.http.Resource;
import com.jslib.tiny.container.mvc.annotation.Controller;
import com.jslib.tiny.container.spi.IClassPostLoadedProcessor;
import com.jslib.tiny.container.spi.IConnector;
import com.jslib.tiny.container.spi.IContainer;
import com.jslib.tiny.container.spi.IManagedClass;
import com.jslib.tiny.container.spi.IManagedMethod;

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
