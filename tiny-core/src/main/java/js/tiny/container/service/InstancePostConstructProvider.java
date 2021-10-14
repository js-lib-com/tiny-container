package js.tiny.container.service;

import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IContainerServiceProvider;

public class InstancePostConstructProvider implements IContainerServiceProvider {
	@Override
	public IContainerService getService(IContainer container) {
		return new InstancePostConstructProcessor();
	}
}
