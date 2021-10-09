package js.tiny.container.mvc;

import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IContainerServiceProvider;

public class ResourceServiceProvider implements IContainerServiceProvider {
	@Override
	public IContainerService getService(IContainer container) {
		return new ResourceService();
	}
}
