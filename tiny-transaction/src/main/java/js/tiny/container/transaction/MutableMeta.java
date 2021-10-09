package js.tiny.container.transaction;

import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.ServiceMeta;

class MutableMeta extends ServiceMeta {
	protected MutableMeta(IContainerService service) {
		super(service);
	}
}
