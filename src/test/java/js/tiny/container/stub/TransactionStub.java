package js.tiny.container.stub;

import js.transaction.Transaction;

public class TransactionStub implements Transaction {
	@Override
	public boolean close() {
		throw new UnsupportedOperationException("close()");
	}

	@Override
	public void commit() {
		throw new UnsupportedOperationException("commit()");
	}

	@Override
	public <T> T getSession() {
		throw new UnsupportedOperationException("getSession()");
	}

	@Override
	public void rollback() {
		throw new UnsupportedOperationException("rollback()");
	}

	@Override
	public boolean unused() {
		throw new UnsupportedOperationException("unused()");
	}
}
