package js.tiny.container.spi;

public interface IInvocationProcessor extends IJoinPointProcessor {
	
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
	Object executeService(IInvocationProcessorsChain chain, IInvocation invocation) throws Exception;
	
}
