package js.tiny.container.mvc;

import java.util.ArrayList;
import java.util.List;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.mvc.annotation.Controller;
import js.tiny.container.mvc.annotation.RequestPath;
import js.tiny.container.mvc.annotation.ResponseContentType;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IContainerServiceMeta;

final class ResourceService implements IContainerService {
	private static final Log log = LogFactory.getLog(ResourceService.class);

	public ResourceService() {
		log.trace("ResourceService()");
	}

	@Override
	public List<IContainerServiceMeta> scan(IManagedClass managedClass) {
		List<IContainerServiceMeta> serviceMetas = new ArrayList<>();

		Controller controller = managedClass.getAnnotation(Controller.class);
		if (controller != null) {
			serviceMetas.add(new ControllerMeta(controller));
		}

		return serviceMetas;
	}

	@Override
	public List<IContainerServiceMeta> scan(IManagedMethod managedMethod) {
		List<IContainerServiceMeta> serviceMetas = new ArrayList<>();

		RequestPath requestPath = managedMethod.getAnnotation(RequestPath.class);
		if (requestPath != null) {
			serviceMetas.add(new RequestPathMeta(requestPath));
		}

		ResponseContentType responseContentType = managedMethod.getAnnotation(ResponseContentType.class);
		if (responseContentType != null) {
			serviceMetas.add(new ResponseContentTypeMeta(responseContentType));
		}

		return serviceMetas;
	}

	@Override
	public void destroy() {
		log.trace("destroy()");
	}
}
