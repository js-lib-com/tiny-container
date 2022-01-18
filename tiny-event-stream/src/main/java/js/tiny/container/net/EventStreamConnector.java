package js.tiny.container.net;

import js.tiny.container.spi.IConnector;
import js.tiny.container.spi.IContainer;

/**
 * Tiny Container service interface.
 * 
 * @author Iulian Rotaru
 */
public class EventStreamConnector implements IConnector {
	@Override
	public void configure(IContainer container) {
		container.bind(EventStream.class).build();
		container.bind(EventStreamManager.class).to(EventStreamManagerImpl.class).build();
	}
}
