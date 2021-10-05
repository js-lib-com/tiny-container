package js.tiny.container.spi;

import js.lang.InvocationException;

public interface IMethodInvocationProcessorsChain {

	Object invokeNextProcessor(IMethodInvocation methodInvocation) throws AuthorizationException, IllegalArgumentException, InvocationException;

}
