package js.tiny.container.security;

import javax.annotation.security.RolesAllowed;

import js.tiny.container.spi.IContainerServiceMeta;

public class RolesAllowedMeta implements IContainerServiceMeta {
	private final String[] value;

	public RolesAllowedMeta(RolesAllowed rolesAllowed) {
		this.value = rolesAllowed.value();
	}

	public String[] value() {
		return value;
	}
}
