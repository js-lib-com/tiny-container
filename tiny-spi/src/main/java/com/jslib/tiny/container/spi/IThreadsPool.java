package com.jslib.tiny.container.spi;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public interface IThreadsPool {

	void execute(String commandName, Command command);

	<T> Future<T> submit(String taskName, Callable<T> task);

	@FunctionalInterface
	static interface Command {
		void call() throws Exception;
	}
}
