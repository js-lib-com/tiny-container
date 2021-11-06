package js.tiny.container.rest;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IConnector;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IAnnotationsScanner;

public class RestConnector implements IConnector, IAnnotationsScanner {
	private static final Log log = LogFactory.getLog(RestConnector.class);

	public RestConnector() {
		log.trace("RestConnector()");
	}

	@Override
	public List<Annotation> scanClassAnnotations(IManagedClass<?> managedClass) {
		List<Annotation> serviceMetas = new ArrayList<>();

		Path path = managedClass.scanAnnotation(Path.class);
		if (path != null) {
			serviceMetas.add(path);
		}

		return serviceMetas;
	}

	@Override
	public List<Annotation> scanMethodAnnotations(IManagedMethod managedMethod) {
		List<Annotation> serviceMetas = new ArrayList<>();

		Path path = managedMethod.scanAnnotation(Path.class);
		if (path != null) {
			serviceMetas.add(path);
		}

		Produces produces = managedMethod.scanAnnotation(Produces.class);
		if (produces != null) {
			serviceMetas.add(produces);
		}

		return serviceMetas;
	}
}
