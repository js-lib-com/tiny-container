package js.tiny.container.rest;

import javax.ws.rs.Produces;

import js.tiny.container.spi.IServiceMeta;

class ProducesMeta implements IServiceMeta {
	private final String value;

	public ProducesMeta(Produces produces) {
		this.value = produces.value().length > 0 ? produces.value()[0] : null;
	}

	public String value() {
		return value;
	}
}
