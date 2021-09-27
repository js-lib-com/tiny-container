package js.tiny.container;

/**
 * Task executed when given timeout expires. This interface is designed to work in conjunction with {@link Timer} like in
 * snippet below.
 * <pre>
 * Timer timer = Factory.getInstance(Timer.class);
 * timer.timeout(new TimeoutTask(){
 * 	void onPeriod() {
 * 		. . .
 * 	}
 * }, timeout);
 * </pre>
 * 
 * 
 * @author Iulian Rotaru
 * @version draft
 */
public interface TimeoutTask {
	/**
	 * Hook to be executed when timeout expires. Implementation should avoid lengthly processing in order to avoid hogging the
	 * scheduler.
	 * 
	 * @throws Exception task logic exceptions are bubbled-up.
	 */
	void onTimeout() throws Exception;
}
