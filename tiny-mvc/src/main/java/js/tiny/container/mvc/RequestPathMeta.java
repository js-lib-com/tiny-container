package js.tiny.container.mvc;

import js.tiny.container.mvc.annotation.RequestPath;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.ServiceMeta;

class RequestPathMeta extends ServiceMeta {
	private final String value;

	public RequestPathMeta(IContainerService service, RequestPath requestPath) {
		super(service);
		this.value = requestPath.value();
	}

	public RequestPathMeta(IContainerService service, String value) {
		super(service);
		this.value = value;
	}

	public String value() {
		return value;
	}
}
