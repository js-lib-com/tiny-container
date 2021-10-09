package js.tiny.container.security;

import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.ServiceMeta;

public class DenyAllMeta extends ServiceMeta {
	protected DenyAllMeta(IContainerService service) {
		super(service);
	}
}
