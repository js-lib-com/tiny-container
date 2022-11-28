package com.jslib.loadbalancer;

class Node implements INode {
	private final String implementationURL;
	private volatile int priority;

	private volatile int state;

	private volatile int usageCount;
	private volatile long usageTime;
	private volatile int errorsCount;

	private volatile long startTimestamp;

	public Node(INodeConfig config) {
		this.implementationURL = config.getImplementationURL();
		this.priority = config.getPriority();

		this.state = State.AVAILABLE.ordinal();
	}

	@Override
	public void acquire() {
		state = State.BUSY.ordinal();
		startTimestamp = System.currentTimeMillis();
		++usageCount;
	}

	@Override
	public long release() {
		long processingTime = System.currentTimeMillis() - startTimestamp;
		usageTime += processingTime;
		state = State.AVAILABLE.ordinal();
		return processingTime;
	}

	@Override
	public void incrementsErrorsCount() {
		++errorsCount;
	}

	@Override
	public String getImplementationURL() {
		return implementationURL;
	}

	@Override
	public State getState() {
		return State.values()[state];
	}

	@Override
	public int getPriority() {
		return priority;
	}

	@Override
	public int getUsageCount() {
		return usageCount;
	}

	@Override
	public long getUsageTime() {
		return usageTime;
	}

	@Override
	public int getErrorsCount() {
		return errorsCount;
	}

	public enum State {
		AVAILABLE, BUSY, DISABLED, ERROR
	}
}
