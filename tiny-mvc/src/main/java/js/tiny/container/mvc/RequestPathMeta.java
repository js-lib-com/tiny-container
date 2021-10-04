package js.tiny.container.mvc;

import js.tiny.container.mvc.annotation.RequestPath;
import js.tiny.container.spi.IServiceMeta;

class RequestPathMeta implements IServiceMeta {
	private final String value;

	public RequestPathMeta(RequestPath requestPath) {
		this.value = requestPath.value();
	}

	public RequestPathMeta(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}
}
