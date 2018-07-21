package js.container;

/**
 * Method invocation meter. This meter is associated to an instrumented method and monitor processing time, invocations and
 * errors count. There is an invocation meter instance for every managed method.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface InvocationMeter extends Meter {
	/**
	 * Get instrumented method declaring class. This is the class where instrumented method is declared. It can be useful to
	 * filter meters by specific class(es).
	 * 
	 * @return method declaring class.
	 */
	Class<?> getMethodDeclaringClass();

	/**
	 * Return instrumented method short signature. Returned signature does not contains declaring class, since it is supplied by
	 * {@link #getMethodDeclaringClass()}; it contains only method name and formal parameters list. It is intended as a visual
	 * clue to related counters.
	 * 
	 * @return method signature.
	 */
	String getMethodSignature();

	/**
	 * Get current value of invocations count. This counter is updated on every method execution, no mater successful or ending
	 * in exception.
	 * 
	 * @return invocations count.
	 */
	long getInvocationsCount();

	/**
	 * Get current value of exception count. This counter records all failed method execution.
	 * 
	 * @return exceptions count.
	 */
	long getExceptionsCount();

	/**
	 * Get total processing time. Keep the sum of all method processing time, including those ending in exception.
	 * 
	 * @return total processing time.
	 */
	long getTotalProcessingTime();

	/**
	 * Get maximum recorded value for processing time. This counter records the maximum value of method processing time, no
	 * matter method was successful or ending in exception.
	 * 
	 * @return maximum recorded value for processing time.
	 */
	long getMaxProcessingTime();

	/**
	 * Reset meter internal state.
	 */
	void reset();
}