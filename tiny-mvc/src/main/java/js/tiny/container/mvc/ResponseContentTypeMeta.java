package js.tiny.container.mvc;

import js.tiny.container.mvc.annotation.ResponseContentType;
import js.tiny.container.spi.IContainerServiceMeta;

class ResponseContentTypeMeta implements IContainerServiceMeta {
	private final String value;

	public ResponseContentTypeMeta(ResponseContentType responseContentType) {
		this.value = responseContentType.value();
	}

	public String value() {
		return value;
	}
}
