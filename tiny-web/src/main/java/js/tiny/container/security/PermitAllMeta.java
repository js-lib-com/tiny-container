package js.tiny.container.security;

import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.ServiceMeta;

public class PermitAllMeta extends ServiceMeta {
	protected PermitAllMeta(IContainerService service) {
		super(service);
	}
}
