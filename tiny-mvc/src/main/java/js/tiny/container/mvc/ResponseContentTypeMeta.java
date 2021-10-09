package js.tiny.container.mvc;

import js.tiny.container.mvc.annotation.ResponseContentType;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.ServiceMeta;

class ResponseContentTypeMeta extends ServiceMeta {
	private final String value;

	public ResponseContentTypeMeta(IContainerService service, ResponseContentType responseContentType) {
		super(service);
		this.value = responseContentType.value();
	}

	public String value() {
		return value;
	}
}
