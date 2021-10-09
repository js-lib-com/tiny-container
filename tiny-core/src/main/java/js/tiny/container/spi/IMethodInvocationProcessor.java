package js.tiny.container.spi;

import js.lang.InvocationException;

public interface IMethodInvocationProcessor extends IJoinPointProcessor {

	Priority getPriority();

	Object invoke(IMethodInvocationProcessorsChain serviceChain, IMethodInvocation methodInvocation) throws AuthorizationException, IllegalArgumentException, InvocationException;

	public static enum Priority {
		SECURITY, PERFMON, FIRST, HIGH, NORMAL, LOW, LAST, NONE
	}
}
