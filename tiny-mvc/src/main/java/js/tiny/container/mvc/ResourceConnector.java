package js.tiny.container.mvc;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.mvc.annotation.Controller;
import js.tiny.container.mvc.annotation.RequestPath;
import js.tiny.container.mvc.annotation.ResponseContentType;
import js.tiny.container.spi.IConnector;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IAnnotationsScanner;

public class ResourceConnector implements IConnector, IAnnotationsScanner {
	private static final Log log = LogFactory.getLog(ResourceConnector.class);

	public ResourceConnector() {
		log.trace("ResourceService()");
	}

	@Override
	public List<Annotation> scanClassAnnotations(IManagedClass<?> managedClass) {
		List<Annotation> serviceMetas = new ArrayList<>();

		Controller controller = managedClass.scanAnnotation(Controller.class);
		if (controller != null) {
			serviceMetas.add(controller);
		}

		return serviceMetas;
	}

	@Override
	public List<Annotation> scanMethodAnnotations(IManagedMethod managedMethod) {
		List<Annotation> serviceMetas = new ArrayList<>();

		RequestPath requestPath = managedMethod.scanAnnotation(RequestPath.class);
		if (requestPath != null) {
			serviceMetas.add(requestPath);
		}

		ResponseContentType responseContentType = managedMethod.scanAnnotation(ResponseContentType.class);
		if (responseContentType != null) {
			serviceMetas.add(responseContentType);
		}

		return serviceMetas;
	}
}
