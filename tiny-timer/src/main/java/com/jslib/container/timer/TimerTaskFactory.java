package com.jslib.container.timer;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class TimerTaskFactory implements ThreadFactory {
	private final AtomicInteger index = new AtomicInteger();
	private final ThreadGroup group = Thread.currentThread().getThreadGroup();
	
	@Override
	public Thread newThread(Runnable runnable) {
		Thread thread = new Thread(group, runnable, String.format("timer-task-%d", index.getAndIncrement()));
		thread.setDaemon(false);
		thread.setPriority(Thread.NORM_PRIORITY);
		return thread;
	}
}
