package js.tiny.container.interceptor;

import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IContainerServiceProvider;

public class InterceptorServiceProvider implements IContainerServiceProvider {
	@Override
	public IContainerService getService(IContainer container) {
		return new InterceptorService(container);
	}
}
