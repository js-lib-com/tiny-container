package js.tiny.container.timer;

import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IContainerServiceProvider;

public class CalendarTimerServiceProvider implements IContainerServiceProvider {
	@Override
	public IContainerService getService(IContainer container) {
		return new CalendarTimerService();
	}
}
