package js.tiny.container.mvc;

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
import js.tiny.container.spi.IServiceMeta;
import js.tiny.container.spi.IServiceMetaScanner;

final class ResourceConnector implements IConnector, IServiceMetaScanner {
	private static final Log log = LogFactory.getLog(ResourceConnector.class);

	public ResourceConnector() {
		log.trace("ResourceService()");
	}

	@Override
	public List<IServiceMeta> scanServiceMeta(IManagedClass managedClass) {
		List<IServiceMeta> serviceMetas = new ArrayList<>();

		Controller controller = managedClass.getAnnotation(Controller.class);
		if (controller != null) {
			serviceMetas.add(new ControllerMeta(this, controller));
		}

		return serviceMetas;
	}

	@Override
	public List<IServiceMeta> scanServiceMeta(IManagedMethod managedMethod) {
		List<IServiceMeta> serviceMetas = new ArrayList<>();

		RequestPath requestPath = managedMethod.getAnnotation(RequestPath.class);
		if (requestPath != null) {
			serviceMetas.add(new RequestPathMeta(this, requestPath));
		}

		ResponseContentType responseContentType = managedMethod.getAnnotation(ResponseContentType.class);
		if (responseContentType != null) {
			serviceMetas.add(new ResponseContentTypeMeta(this, responseContentType));
		}

		return serviceMetas;
	}
}
