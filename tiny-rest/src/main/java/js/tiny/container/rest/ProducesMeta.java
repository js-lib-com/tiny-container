package js.tiny.container.rest;

import javax.ws.rs.Produces;

import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.ServiceMeta;

class ProducesMeta extends ServiceMeta {
	private final String value;

	public ProducesMeta(IContainerService service, Produces produces) {
		super(service);
		this.value = produces.value().length > 0 ? produces.value()[0] : null;
	}

	public String value() {
		return value;
	}
}
