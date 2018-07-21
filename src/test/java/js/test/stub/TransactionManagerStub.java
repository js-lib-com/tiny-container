package js.test.stub;

import js.lang.Config;
import js.transaction.Transaction;
import js.transaction.TransactionManager;
import js.transaction.WorkingUnit;

public class TransactionManagerStub implements TransactionManager {
	@Override
	public void config(Config config) throws Exception {
		throw new UnsupportedOperationException("config(Config)");
	}

	@Override
	public Transaction createReadOnlyTransaction() {
		throw new UnsupportedOperationException("createReadOnlyTransaction()");
	}

	@Override
	public Transaction createTransaction() {
		throw new UnsupportedOperationException("createTransaction()");
	}

	@Override
	public void destroy() {
		throw new UnsupportedOperationException("destroy()");
	}

	@Override
	public <S, T> T exec(WorkingUnit<S, T> workingUnit, Object... args) {
		throw new UnsupportedOperationException("exec(WorkingUnit<S, T>, Object...)");
	}
}
