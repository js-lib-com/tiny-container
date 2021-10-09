package js.tiny.container.perfmon;

import java.util.Collections;
import java.util.List;

import js.lang.InvocationException;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IMethodInvocation;
import js.tiny.container.spi.IMethodInvocationProcessor;
import js.tiny.container.spi.IMethodInvocationProcessorsChain;
import js.tiny.container.spi.IServiceMeta;

class PerformanceMonitorService implements IContainerService, IMethodInvocationProcessor {
	private static final Log log = LogFactory.getLog(PerformanceMonitorService.class);

	private static final String ATTR_METER = "meter";

	public PerformanceMonitorService() {
		log.trace("PerformanceMonitorService()");
	}

	@Override
	public Priority getPriority() {
		return Priority.PERFMON;
	}

	@Override
	public List<IServiceMeta> scan(IManagedClass managedClass) {
		managedClass.getManagedMethods().forEach(managedMethod -> {
			managedMethod.setAttribute(getClass(), ATTR_METER, new Meter(managedMethod));
		});
		return Collections.emptyList();
	}

	@Override
	public List<IServiceMeta> scan(IManagedMethod managedMethod) {
		return Collections.emptyList();
	}

	@Override
	public Object invoke(IMethodInvocationProcessorsChain serviceChain, IMethodInvocation methodInvocation) throws AuthorizationException, IllegalArgumentException, InvocationException {
		Meter meter = methodInvocation.method().getAttribute(getClass(), ATTR_METER, Meter.class);
		meter.incrementInvocationsCount();
		meter.startProcessing();

		Object value = null;
		try {

			value = serviceChain.invokeNextProcessor(methodInvocation);

		} catch (Exception e) {
			meter.incrementExceptionsCount();
			throw e;
		} finally {
			meter.stopProcessing();
		}

		return value;
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}
}
