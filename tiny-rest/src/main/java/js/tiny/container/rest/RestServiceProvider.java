package js.tiny.container.rest;

import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IContainerServiceProvider;

public class RestServiceProvider implements IContainerServiceProvider {
	@Override
	public IContainerService createService(IContainer container) {
		return new RestService();
	}
}
