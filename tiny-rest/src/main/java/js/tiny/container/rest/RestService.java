package js.tiny.container.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IContainerServiceMeta;

public class RestService implements IContainerService {
	private static final Log log = LogFactory.getLog(RestService.class);

	public RestService() {
		log.trace("RestService()");
	}

	@Override
	public List<IContainerServiceMeta> scan(IManagedClass managedClass) {
		List<IContainerServiceMeta> serviceMetas = new ArrayList<>();

		Path path = managedClass.getAnnotation(Path.class);
		if (path != null) {
			serviceMetas.add(new PathMeta(path));
		}

		return serviceMetas;
	}

	@Override
	public List<IContainerServiceMeta> scan(IManagedMethod managedMethod) {
		List<IContainerServiceMeta> serviceMetas = new ArrayList<>();

		Path path = managedMethod.getAnnotation(Path.class);
		if (path != null) {
			serviceMetas.add(new PathMeta(path));
		}

		Produces produces = managedMethod.getAnnotation(Produces.class);
		if (produces != null) {
			serviceMetas.add(new ProducesMeta(produces));
		}

		return serviceMetas;
	}

	@Override
	public void destroy() {
		log.trace("destroy()");

	}
}
