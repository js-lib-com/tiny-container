package js.tiny.container.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IConnector;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IServiceMeta;
import js.tiny.container.spi.IServiceMetaScanner;

class RestConnector implements IConnector, IServiceMetaScanner {
	private static final Log log = LogFactory.getLog(RestConnector.class);

	public RestConnector() {
		log.trace("RestConnector()");
	}

	@Override
	public List<IServiceMeta> scanServiceMeta(IManagedClass managedClass) {
		List<IServiceMeta> serviceMetas = new ArrayList<>();

		Path path = managedClass.getAnnotation(Path.class);
		if (path != null) {
			serviceMetas.add(new PathMeta(this, path));
		}

		return serviceMetas;
	}

	@Override
	public List<IServiceMeta> scanServiceMeta(IManagedMethod managedMethod) {
		List<IServiceMeta> serviceMetas = new ArrayList<>();

		Path path = managedMethod.getAnnotation(Path.class);
		if (path != null) {
			serviceMetas.add(new PathMeta(this, path));
		}

		Produces produces = managedMethod.getAnnotation(Produces.class);
		if (produces != null) {
			serviceMetas.add(new ProducesMeta(this, produces));
		}

		return serviceMetas;
	}
}
