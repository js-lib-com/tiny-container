package js.tiny.container.contextparam;

import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IContainerServiceProvider;

public class ClassContextParamProvider implements IContainerServiceProvider {
	@Override
	public IContainerService getService(IContainer container) {
		return new ClassContextParam(container);
	}
}
