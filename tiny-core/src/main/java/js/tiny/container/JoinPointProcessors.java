package js.tiny.container;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import js.lang.BugError;
import js.tiny.container.spi.IJoinPointProcessor;

class JoinPointProcessors<T extends IJoinPointProcessor> implements Iterable<T> {
	private final SortedSet<T> processors;

	public JoinPointProcessors() {
		this.processors = new TreeSet<>((p1, p2) -> {
			if (p1.getPriority() == p2.getPriority()) {
				return p1.getClass().getSimpleName().compareTo(p2.getClass().getSimpleName());
			}
			return Integer.compare(p1.getPriority(), p2.getPriority());
		});
	}

	public void add(T processor) {
		// allow only one processor instance per type
		for (T existingClassProcessoor : processors) {
			if (existingClassProcessoor.getClass().equals(processor.getClass())) {
				throw new BugError("Attempt to override class processor |%s|.", processor.getClass());
			}
		}

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
