package js.tiny.container.perfmon;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import js.tiny.container.spi.IManagedMethod;

public class MetersStore {
	private static final Object mutex = new Object();
	private static MetersStore instance;

	public static MetersStore instance() {
		if (instance == null) {
			synchronized (mutex) {
				if (instance == null) {
					instance = new MetersStore();
				}
			}
		}
		return instance;
	}

	private final Map<IManagedMethod, IInvocationMeter> meters = new HashMap<>();

	public void createMeter(IManagedMethod managedMethod) {
		meters.put(managedMethod, new Meter(managedMethod));
	}

	public Meter getMeter(IManagedMethod method) {
		return (Meter) meters.get(method);
	}

	public SortedSet<IInvocationMeter> getInvocationMeters() {
		SortedSet<IInvocationMeter> invocationMeters = new TreeSet<>((m1, m2) -> ((Long) m1.getMaxProcessingTime()).compareTo(m2.getMaxProcessingTime()));
		for (IInvocationMeter meter : meters.values()) {
			if (meter.getInvocationsCount() > 0) {
				invocationMeters.add(meter);
			}
		}
		return invocationMeters;
	}
}
