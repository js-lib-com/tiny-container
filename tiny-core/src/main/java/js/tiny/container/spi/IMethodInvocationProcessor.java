package js.tiny.container.spi;

/**
 * Container services related to application methods invocation. Join point for this services is the
 * {@link IManagedMethod#invoke(Object, Object...)} method; here application logic execution cross-cuts container services
 * related to method invocation.
 * 
 * When an application method should be executed container routes request to this join point. Here invocation processors chain
 * is created and executed; this way all container services implementing this interface are executed, before the actual
 * application method execution.
 * 
 * Invocation processors execution order matters. For example security services should be applied first, for obvious reasons. By
 * contrast transaction should be the last one, executed directly around application method. Invocation processor implementation
 * should select a suitable predefined priority declared by {@link Priority}.
 * 
 * @author Iulian Rotaru
 */
public interface IMethodInvocationProcessor extends IFlowProcessor {

	/**
	 * Execute container service logic implemented by current invocation processor then invoke the next processor from chain.
	 * Current service logic can be executed before, after or around next processor logic. It is legal for current processor to
	 * interrupt processing chain, that is, not to call the next processor from chain.
	 * 
	 * @param chain invocation processors chain,
	 * @param invocation method invocation context.
	 * @return value returned by next processor from chain or null if next processor is not executed.
	 * @throws Exception if service processing fails for whatever reason.
	 */
	Object onMethodInvocation(IInvocationProcessorsChain chain, IInvocation invocation) throws Exception;

	Priority getPriority();

	/**
	 * Predefined priorities available to invocation processor implementations.
	 * 
	 * @author Iulian Rotaru
	 */
	enum Priority implements IPriority {
		/** 0 - authorized access to application methods */
		SECURITY,
		/** 1 - asynchronous method execution */
		ASYNCHRONOUS,
		/** 2 - performance counters and measurements */
		PERFMON,
		/** 3 - application defined method invocation interceptors */
		INTERCEPTOR,
		/** 4 - transactional boundaries just around application method */
		TRANSACTION,
		/** 5 - application method execution is the last */
		METHOD
	}

}
