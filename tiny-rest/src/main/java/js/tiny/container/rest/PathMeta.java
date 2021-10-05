package js.tiny.container.rest;

import javax.ws.rs.Path;

import js.tiny.container.spi.IContainerServiceMeta;

class PathMeta implements IContainerServiceMeta {
	private final String value;

	public PathMeta(Path path) {
		this.value = path.value();
	}

	public PathMeta(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}
}
