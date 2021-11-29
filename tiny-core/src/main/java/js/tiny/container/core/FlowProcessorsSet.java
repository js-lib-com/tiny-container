package js.tiny.container.core;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import js.tiny.container.spi.IFlowProcessor;

/**
 * An set of flow processors ordered by priority, see {@link IFlowProcessor#getPriority()}. A set of processors is attached to a
 * particular container flow point.
 * 
 * @author Iulian Rotaru
 */
public class FlowProcessorsSet<T extends IFlowProcessor> implements Iterable<T> {
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

	public void clear() {
		processors.clear();
	}
}
