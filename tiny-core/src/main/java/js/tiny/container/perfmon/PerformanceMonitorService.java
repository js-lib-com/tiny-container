package js.tiny.container.perfmon;

import java.util.ArrayList;
import java.util.List;

import js.lang.InvocationException;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IContainerServiceMeta;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IMethodInvocation;
import js.tiny.container.spi.IMethodInvocationProcessor;
import js.tiny.container.spi.IMethodInvocationProcessorsChain;

class PerformanceMonitorService implements IContainerService, IMethodInvocationProcessor {
	private static final Log log = LogFactory.getLog(PerformanceMonitorService.class);

	public PerformanceMonitorService() {
		log.trace("PerformanceMonitorService()");
	}

	@Override
	public Priority getPriority() {
		return Priority.FIRST;
	}

	@Override
	public List<IContainerServiceMeta> scan(IManagedClass managedClass) {
		List<IContainerServiceMeta> servicesMeta = new ArrayList<>();
		return servicesMeta;
	}

	@Override
	public List<IContainerServiceMeta> scan(IManagedMethod managedMethod) {
		List<IContainerServiceMeta> servicesMeta = new ArrayList<>();

		servicesMeta.add(new Meter(managedMethod));

		return servicesMeta;
	}

	@Override
	public Object invoke(IMethodInvocationProcessorsChain serviceChain, IMethodInvocation methodInvocation) throws AuthorizationException, IllegalArgumentException, InvocationException {
		Meter meter = methodInvocation.method().getServiceMeta(Meter.class);
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
