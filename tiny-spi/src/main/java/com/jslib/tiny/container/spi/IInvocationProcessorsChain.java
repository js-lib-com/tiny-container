package com.jslib.tiny.container.spi;

/**
 * A chain of {@link IMethodInvocationProcessor} executed in sequence in a nested manner. Current processor logic is executed
 * before, after or around next processor logic. Executing processor has a reference to the chain and is responsible for
 * invoking next processor via {@link #invokeNextProcessor(IInvocation)}. Last processor is always {@link IManagedMethod} that
 * does not invoke next processor, this way interrupting this processing chain.
 * 
 * It is legal for an intermediate processor to break processing chain prematurely by refraining to invoke next processor. Also
 * an processor may throw exception concluding in processing chain abort.
 * 
 * @author Iulian Rotaru
 */
public interface IInvocationProcessorsChain {

	Object invokeNextProcessor(IInvocation invocation) throws Exception;

}
