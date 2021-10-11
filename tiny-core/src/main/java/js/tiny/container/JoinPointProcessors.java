package js.tiny.container;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import js.tiny.container.spi.IJoinPointProcessor;

class JoinPointProcessors<T extends IJoinPointProcessor> implements Iterable<T> {
	private final SortedSet<T> processors;

	public JoinPointProcessors() {
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

	@Override
	public Iterator<T> iterator() {
		return processors.iterator();
	}

	public void clear() {
		processors.clear();
	}
}
