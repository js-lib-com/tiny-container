package com.jslib.tiny.container.rest;

import java.lang.reflect.Field;
import java.util.List;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.tiny.container.http.Resource;
import com.jslib.tiny.container.rest.sse.SseEventSinkImpl;
import com.jslib.tiny.container.rest.sse.SseImpl;
import com.jslib.tiny.container.spi.IClassPostLoadedProcessor;
import com.jslib.tiny.container.spi.IConnector;
import com.jslib.tiny.container.spi.IContainer;
import com.jslib.tiny.container.spi.IManagedClass;
import com.jslib.tiny.container.spi.IManagedMethod;

import jakarta.ejb.Remote;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import com.jslib.api.json.Json;
import com.jslib.util.Strings;
import com.jslib.util.Types;

public class RestConnector implements IConnector, IClassPostLoadedProcessor {
	private static final Log log = LogFactory.getLog(RestConnector.class);

	private PathMethodsCache pathMethods;
	private ContextInjectionCache contextInjectors;

	@Inject
	public RestConnector() {
	}

	/** Test constructor. */
	public RestConnector(PathMethodsCache pathMethods, ContextInjectionCache contextInjectors) {
		this.pathMethods = pathMethods;
		this.contextInjectors = contextInjectors;
	}

	@Override
	public void configure(IContainer container) {
		container.bind(Json.class).service().build();
		container.bind(PathMethodsCache.class).in(ApplicationScoped.class).build();
		container.bind(ContextInjectionCache.class).in(ApplicationScoped.class).build();

		container.bind(Sse.class).to(SseImpl.class).in(ApplicationScoped.class).build();
		// WARN: is critical that SSE event sink to have request scope
		container.bind(SseEventSink.class).to(SseEventSinkImpl.class).in(RequestScoped.class).build();
	}

	@Override
	public void create(IContainer container) {
		pathMethods = container.getInstance(PathMethodsCache.class);
		contextInjectors = container.getInstance(ContextInjectionCache.class);
	}

	@Override
	public <T> boolean bind(IManagedClass<T> managedClass) {
		return managedClass.scanAnnotation(Remote.class) != null;
	}

	@Override
	public Priority getPriority() {
		return Priority.SCAN;
	}

	@Override
	public <T> boolean onClassPostLoaded(IManagedClass<T> managedClass) {
		if (managedClass.scanAnnotation(Remote.class) == null) {
			return false;
		}

		log.debug("Scan REST controller |%s|.", managedClass);
		Class<? extends T> implementationClass = managedClass.getImplementationClass();

		for (Field field : implementationClass.getDeclaredFields()) {
			if (field.getAnnotation(Context.class) != null) {
				IMemberInjector injector = new IMemberInjector.FieldInjector(field);
				injector.assertValid();
				log.debug("Register context injector for field |%s|.", field);
				contextInjectors.add(implementationClass, injector);
			}
		}

		for (IManagedMethod managedMethod : managedClass.getManagedMethods()) {
			if (managedMethod.scanAnnotation(Context.class) != null) {
				IMemberInjector injector = new IMemberInjector.MethodInjector(managedMethod);
				injector.assertValid();
				log.debug("Register context injector for method |%s|.", managedMethod);
				contextInjectors.add(implementationClass, injector);
			}
			if (managedMethod.isPublic() && !Types.isKindOf(managedMethod.getReturnType(), Resource.class)) {
				List<String> paths = pathMethods.add(managedMethod);
				log.debug("Register REST method |%s| to path |%s|.", managedMethod, Strings.join(paths, ','));
			}
		}

		return true;
	}
}
