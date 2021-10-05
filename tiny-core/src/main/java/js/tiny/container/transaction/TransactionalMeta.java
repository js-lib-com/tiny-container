package js.tiny.container.transaction;

import js.tiny.container.spi.IContainerServiceMeta;
import js.transaction.Transactional;

class TransactionalMeta implements IContainerServiceMeta {
	private final String schema;

	public TransactionalMeta(Transactional transactional) {
		this.schema = transactional.schema().isEmpty() ? null : transactional.schema();
	}

	public String schema() {
		return schema;
	}
}
