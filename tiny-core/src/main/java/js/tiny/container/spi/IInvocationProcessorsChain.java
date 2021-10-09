package js.tiny.container.spi;

public interface IInvocationProcessorsChain {

	Object invokeNextProcessor(IInvocation invocation) throws Exception;

}
