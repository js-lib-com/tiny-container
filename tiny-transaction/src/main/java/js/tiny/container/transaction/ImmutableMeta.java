package js.tiny.container.transaction;

import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.ServiceMeta;

class ImmutableMeta extends ServiceMeta {
	protected ImmutableMeta(IContainerService service) {
		super(service);
	}
}
