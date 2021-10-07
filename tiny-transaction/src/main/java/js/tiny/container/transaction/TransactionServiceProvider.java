package js.tiny.container.transaction;

import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IContainerServiceProvider;

public class TransactionServiceProvider implements IContainerServiceProvider {
	@Override
	public IContainerService createService(IContainer container) {
		return new TransactionService(container);
	}
}
