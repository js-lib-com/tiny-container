package com.jslib.tiny.container.perfmon;

import com.jslib.tiny.container.spi.IManagedMethod;

/**
 * Invocation meters implementation for managed methods. Standard usage pattern is to update meter state on
 * {@link IManagedMethod#invoke(Object, Object...)} execution, like in sample code below. Also, not present in pseudo-code,
 * increment exceptions count if method logic fails.
 * 
 * <pre>
 * class ManagedMethod {
 * 	void invoke() {
 * 		this.meter.incrementInvocationsCount();
 * 		this.meter.startProcessing();
 * 		// process method logic
 * 		this.meter.stopProcessing();
 * 	}
 * }
 * </pre>
 * 
 * Managed method meter class implements {@link IInvocationMeter} interface for meter counters reading.
 * 
 * @author Iulian Rotaru
 */
class Meter implements IInvocationMeter {
	public static final String ATTR_METER = "meter";

	/** Instrumented method signature. */
	private String methodSignature;

	/** Method invocations count. Updated by {@link #incrementInvocationsCount()}. */
	private long invocationsCount;

	/** Method exceptions count. Updated by {@link #incrementExceptionsCount()}. */
	private long exceptionsCount;

	/** Total processing time. */
	private long totalProcessingTime;

	/** Maximum value of processing time. */
	private long maxProcessingTime;

	/** Timestamp for processing time recording start. */
	private long startProcessingTimestamp;

	/**
	 * Construct meter instance. Store declaring class and initialize method signature.
	 * 
	 * @param method instrumented method.
	 */
	public Meter(IManagedMethod method) {
		this.methodSignature = method.getSignature();
	}

	@Override
	public String getMethodSignature() {
		return methodSignature;
	}

	@Override
	public long getInvocationsCount() {
		return invocationsCount;
	}

	@Override
	public long getExceptionsCount() {
		return exceptionsCount;
	}

	@Override
	public long getTotalProcessingTime() {
		return totalProcessingTime;
	}

	@Override
	public long getMaxProcessingTime() {
		return maxProcessingTime;
	}

	@Override
	public void reset() {
		invocationsCount = 0;
		exceptionsCount = 0;
		totalProcessingTime = 0;
		maxProcessingTime = 0;
	}

	@Override
	public String toExternalForm() {
		long totalProcessingTime = this.totalProcessingTime / 1000000;
		long averageProcessingTime = invocationsCount != 0 ? totalProcessingTime / invocationsCount : 0;

		StringBuilder sb = new StringBuilder();
		sb.append(methodSignature);
		sb.append(": ");
		sb.append(invocationsCount);
		sb.append(": ");
		sb.append(exceptionsCount);
		sb.append(": ");
		sb.append(totalProcessingTime);
		sb.append(": ");
		sb.append(maxProcessingTime / 1000000);
		sb.append(": ");
		sb.append(averageProcessingTime);
		return sb.toString();
	}

	/** Increment invocation count on every method invocation, including those failed. */
	void incrementInvocationsCount() {
		++invocationsCount;
	}

	/** Increment exceptions count for every failed method invocation. */
	void incrementExceptionsCount() {
		++exceptionsCount;
	}

	/**
	 * Start recording of processing time. It is called just before method execution. Update internal
	 * {@link #startProcessingTimestamp} to current system time.
	 */
	void startProcessing() {
		startProcessingTimestamp = System.nanoTime();
	}

	/** Stop recording of processing time and update total and maximum processing time. */
	void stopProcessing() {
		long processingTime = System.nanoTime() - startProcessingTimestamp;
		totalProcessingTime += processingTime;
		if (maxProcessingTime < processingTime) {
			maxProcessingTime = processingTime;
		}
	}
}