package js.tiny.container.rest;

import javax.ws.rs.Path;

import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.ServiceMeta;

class PathMeta extends ServiceMeta {
	private final String value;

	public PathMeta(IContainerService service, Path path) {
		super(service);
		this.value = path.value();
	}

	public PathMeta(IContainerService service, String value) {
		super(service);
		this.value = value;
	}

	public String value() {
		return value;
	}
}
