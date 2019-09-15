package js.test.stub;

import js.lang.Config;
import js.lang.ConfigException;
import js.transaction.Transaction;
import js.transaction.TransactionManager;
import js.transaction.WorkingUnit;

public class TransactionManagerStub implements TransactionManager {
	@Override
	public void config(Config config) throws ConfigException {
		throw new UnsupportedOperationException("config(Config)");
	}

	@Override
	public Transaction createReadOnlyTransaction(String schema) {
		throw new UnsupportedOperationException("createReadOnlyTransaction(String schema)");
	}

	@Override
	public Transaction createTransaction(String schema) {
		throw new UnsupportedOperationException("createTransaction(String schema)");
	}

	@Override
	public void destroy() {
		throw new UnsupportedOperationException("destroy()");
	}

	@Override
	public <S, T> T exec(String schema, WorkingUnit<S, T> workingUnit, Object... args) {
		throw new UnsupportedOperationException("exec(String, WorkingUnit<S, T>, Object...)");
	}

	@Override
	public <S, T> T exec(WorkingUnit<S, T> workingUnit, Object... args) {
		throw new UnsupportedOperationException("exec(WorkingUnit<S, T>, Object...)");
	}
}
