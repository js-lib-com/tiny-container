package js.tiny.container.security;

import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IContainerServiceProvider;

public class SecurityServiceProvider implements IContainerServiceProvider {
	@Override
	public IContainerService createService(IContainer container) {
		return new SecurityService(container);
	}
}
