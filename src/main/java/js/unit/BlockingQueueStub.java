package js.unit;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class BlockingQueueStub<E> implements BlockingQueue<E> {
	@Override
	public E remove() {
		throw new UnsupportedOperationException("remove()");
	}

	@Override
	public E poll() {
		throw new UnsupportedOperationException("poll()");
	}

	@Override
	public E element() {
		throw new UnsupportedOperationException("element()");
	}

	@Override
	public E peek() {
		throw new UnsupportedOperationException("peek()");
	}

	@Override
	public int size() {
		throw new UnsupportedOperationException("size()");
	}

	@Override
	public boolean isEmpty() {
		throw new UnsupportedOperationException("isEmpty()");
	}

	@Override
	public Iterator<E> iterator() {
		throw new UnsupportedOperationException("iterator()");
	}

	@Override
	public Object[] toArray() {
		throw new UnsupportedOperationException("toArray()");
	}

	@Override
	public <T> T[] toArray(T[] a) {
		throw new UnsupportedOperationException("toArray(T[] a)");
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		throw new UnsupportedOperationException("containsAll(Collection<?> c)");
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		throw new UnsupportedOperationException("addAll(Collection<? extends E> c)");
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException("removeAll(Collection<?> c)");
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException("retainAll(Collection<?> c)");
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException("clear()");
	}

	@Override
	public boolean add(E e) {
		throw new UnsupportedOperationException("add(E e)");
	}

	@Override
	public boolean offer(E e) {
		throw new UnsupportedOperationException("offer(E e)");
	}

	@Override
	public void put(E e) throws InterruptedException {
		throw new UnsupportedOperationException("put(E e)");
	}

	@Override
	public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
		throw new UnsupportedOperationException("offer(E e, long timeout, TimeUnit unit)");
	}

	@Override
	public E take() throws InterruptedException {
		throw new UnsupportedOperationException("take()");
	}

	@Override
	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		throw new UnsupportedOperationException("poll(long timeout, TimeUnit unit)");
	}

	@Override
	public int remainingCapacity() {
		throw new UnsupportedOperationException("remainingCapacity()");
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException("remove(Object o)");
	}

	@Override
	public boolean contains(Object o) {
		throw new UnsupportedOperationException("contains(Object o)");
	}

	@Override
	public int drainTo(Collection<? super E> c) {
		throw new UnsupportedOperationException("drainTo(Collection<? super E> c)");
	}

	@Override
	public int drainTo(Collection<? super E> c, int maxElements) {
		throw new UnsupportedOperationException("drainTo(Collection<? super E> c, int maxElements)");
	}
}
