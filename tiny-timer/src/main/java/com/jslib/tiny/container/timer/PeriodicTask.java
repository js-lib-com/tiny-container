package com.jslib.tiny.container.timer;

/**
 * Task executed periodically. This interface is designed to work in conjunction with {@link Timer} like in snippet below.
 * <pre>
 * Timer timer = Factory.getInstance(Timer.class);
 * timer.period(new PeriodicTask(){
 * 	void onPeriod() {
 * 		. . .
 * 	}
 * }, period);
 * </pre>
 * 
 * @author Iulian Rotaru
 */
public interface PeriodicTask {
	/**
	 * Hook to be executed periodically. Implementation should avoid lengthly processing in order to avoid hogging the
	 * scheduler.
	 * 
	 * @throws Exception task logic exceptions are bubbled-up.
	 */
	void onPeriod() throws Exception;
}
