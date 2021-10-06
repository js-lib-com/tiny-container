package js.tiny.container.perfmon;

import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IContainerServiceProvider;

public class PerformanceMonitorServiceProvider implements IContainerServiceProvider {
	@Override
	public IContainerService createService(IContainer container) {
		return new PerformanceMonitorService();
	}

}
