package js.tiny.container.transaction;

import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.ServiceMeta;
import js.transaction.Transactional;

class TransactionalMeta extends ServiceMeta {
	private final String schema;

	public TransactionalMeta(IContainerService service, Transactional transactional) {
		super(service);
		this.schema = transactional.schema().isEmpty() ? null : transactional.schema();
	}

	public String schema() {
		return schema;
	}
}
