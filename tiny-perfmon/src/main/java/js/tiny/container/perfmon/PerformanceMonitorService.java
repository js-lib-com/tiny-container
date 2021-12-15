package js.tiny.container.perfmon;

import javax.inject.Singleton;

import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IInvocation;
import js.tiny.container.spi.IInvocationProcessorsChain;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IMethodInvocationProcessor;

public class PerformanceMonitorService implements IMethodInvocationProcessor {
	private final MetersStore metersStore;

	public PerformanceMonitorService() {
		this.metersStore = MetersStore.instance();
	}

	@Override
	public void create(IContainer container) {
		container.bind(Observer.class).in(Singleton.class).build();
	}

	@Override
	public Priority getPriority() {
		return Priority.PERFMON;
	}

	@Override
	public boolean bind(IManagedMethod managedMethod) {
		metersStore.createMeter(managedMethod);
		return true;
	}

	@Override
	public Object onMethodInvocation(IInvocationProcessorsChain chain, IInvocation invocation) throws Exception {
		Meter meter = metersStore.getMeter(invocation.method());
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
