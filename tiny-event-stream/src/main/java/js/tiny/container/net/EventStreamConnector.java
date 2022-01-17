package js.tiny.container.net;

import jakarta.inject.Singleton;
import js.tiny.container.spi.IConnector;
import js.tiny.container.spi.IContainer;

public class EventStreamConnector implements IConnector {
	@Override
	public void configure(IContainer container) {
		container.bind(EventStreamManager.class).to(EventStreamManagerImpl.class).in(Singleton.class).build();
	}
}
