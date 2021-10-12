package js.tiny.container.service;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;

import js.tiny.container.spi.IFlowProcessor;

public class FlowProcessorsSet<T extends IFlowProcessor> {
	private final SortedSet<T> processors;

	public FlowProcessorsSet() {
		this.processors = new TreeSet<>((processor1, processor2) -> {
			final int priority1 = processor1.getPriority().ordinal();
			final int priority2 = processor2.getPriority().ordinal();

			if (priority1 == priority2) {
				return processor1.getClass().getSimpleName().compareTo(processor2.getClass().getSimpleName());
			}
			return Integer.compare(priority1, priority2);
		});
	}

	public void add(T processor) {
		processors.add(processor);
	}

	public Iterator<T> iterator() {
		return processors.iterator();
	}

	public void forEach(Consumer<T> callback) {
		for (T processor : processors) {
			callback.accept(processor);
		}
	}

	public void clear() {
		processors.clear();
	}
}
