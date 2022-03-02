package js.tiny.container.rest;

import java.lang.reflect.Field;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import js.json.Json;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.http.Resource;
import js.tiny.container.rest.sse.SseEventSinkImpl;
import js.tiny.container.rest.sse.SseImpl;
import js.tiny.container.spi.IClassPostLoadedProcessor;
import js.tiny.container.spi.IConnector;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.util.Strings;
import js.util.Types;

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
		return IRemote.scan(managedClass) != null;
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
		Class<? extends T> implementationClass = managedClass.getImplementationClass();

		for (Field field : implementationClass.getDeclaredFields()) {
			if (IContext.scan(field)) {
				IMemberInjector injector = new IMemberInjector.FieldInjector(field);
				injector.assertValid();
				log.debug("Register context injector for field |%s|.", field);
				contextInjectors.add(implementationClass, injector);
			}
		}

		for (IManagedMethod managedMethod : managedClass.getManagedMethods()) {
			if (IContext.scan(managedMethod)) {
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
