package js.tiny.container.security;

import javax.annotation.security.RolesAllowed;

import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.ServiceMeta;

public class RolesAllowedMeta extends ServiceMeta {
	private final String[] value;

	public RolesAllowedMeta(IContainerService service, RolesAllowed rolesAllowed) {
		super(service);
		this.value = rolesAllowed.value();
	}

	public String[] value() {
		return value;
	}
}
