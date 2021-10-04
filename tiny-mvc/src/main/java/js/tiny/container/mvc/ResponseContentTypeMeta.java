package js.tiny.container.mvc;

import js.tiny.container.spi.IServiceMeta;

class ResponseContentTypeMeta implements IServiceMeta {
	private final String value;

	public ResponseContentTypeMeta(ResponseContentType responseContentType) {
		this.value = responseContentType.value();
	}

	public String value() {
		return value;
	}
}
