package com.jslib.container.perfmon;

import com.jslib.container.spi.IContainer;
import com.jslib.container.spi.IInvocation;
import com.jslib.container.spi.IInvocationProcessorsChain;
import com.jslib.container.spi.IManagedMethod;
import com.jslib.container.spi.IMethodInvocationProcessor;

import jakarta.inject.Singleton;

public class PerformanceMonitorService implements IMethodInvocationProcessor {
	private MetersStore meters;

	@Override
	public void configure(IContainer container) {
		container.bind(MetersStore.class).in(Singleton.class).build();
		container.bind(Observer.class).in(Singleton.class).build();
	}

	@Override
	public void create(IContainer container) {
		meters = container.getInstance(MetersStore.class);
	}

	@Override
	public Priority getPriority() {
		return Priority.PERFMON;
	}

	@Override
	public boolean bind(IManagedMethod managedMethod) {
		meters.createMeter(managedMethod);
		return true;
	}

	@Override
	public Object onMethodInvocation(IInvocationProcessorsChain chain, IInvocation invocation) throws Exception {
		Meter meter = meters.getMeter(invocation.method());
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
