package js.tiny.container.perfmon;

import java.util.Collections;
import java.util.List;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IInvocation;
import js.tiny.container.spi.IInvocationProcessor;
import js.tiny.container.spi.IInvocationProcessorsChain;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IServiceMeta;
import js.tiny.container.spi.IServiceMetaScanner;
import js.tiny.container.spi.Priority;

class PerformanceMonitorService implements IInvocationProcessor, IServiceMetaScanner {
	private static final Log log = LogFactory.getLog(PerformanceMonitorService.class);

	private static final String ATTR_METER = "meter";

	public PerformanceMonitorService() {
		log.trace("PerformanceMonitorService()");
	}

	@Override
	public int getPriority() {
		return Priority.PERFMON.value(1);
	}

	@Override
	public List<IServiceMeta> scanServiceMeta(IManagedClass managedClass) {
		managedClass.getManagedMethods().forEach(managedMethod -> {
			managedMethod.setAttribute(getClass(), ATTR_METER, new Meter(managedMethod));
		});
		return Collections.emptyList();
	}

	@Override
	public List<IServiceMeta> scanServiceMeta(IManagedMethod managedMethod) {
		return Collections.emptyList();
	}

	@Override
	public Object executeService(IInvocationProcessorsChain chain, IInvocation invocation) throws Exception {
		Meter meter = invocation.method().getAttribute(getClass(), ATTR_METER, Meter.class);
		meter.incrementInvocationsCount();
		meter.startProcessing();

		Object value = null;
		try {

			value = chain.invokeNextProcessor(invocation);

		} catch (Exception e) {
			meter.incrementExceptionsCount();
			throw e;
		} finally {
			meter.stopProcessing();
		}

		return value;
	}
}
