package js.net.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import js.lang.ManagedPreDestroy;
import js.net.EventStream;
import js.net.EventStreamConfig;
import js.net.EventStreamManager;
import js.net.EventStreamManagerSPI;
import js.test.stub.AppFactoryStub;
import js.util.Classes;

import org.junit.Before;
import org.junit.Test;

@SuppressWarnings({ "unchecked", "hiding" })
public class EventStreamManagerUnitTest {
	private MockAppFactory factory;

	@Before
	public void beforeTest() {
		factory = new MockAppFactory();

		// reset session ID seed to have predictable IDs sequence
		Class<?> SessionIDClass = Classes.forName("js.net.EventStreamManagerImpl$SessionID");
		AtomicInteger ID_SEED = Classes.getFieldValue(SessionIDClass, "ID_SEED");
		ID_SEED.set(0);
	}

	/** Pre-destroy should invoke close on all event streams still opened but should not affect list size. */
	@Test
	public void preDestroy() throws Exception {
		EventStreamManagerSPI manager = Classes.newInstance("js.net.EventStreamManagerImpl", factory);
		int STREAMS_COUNT = 100;

		class MockEventStream extends EventStream {
			private int closeProbe;

			@Override
			public void close() {
				++closeProbe;
				super.close();
			}
		}

		List<MockEventStream> eventStreams = Classes.getFieldValue(manager, "eventStreams");
		for (int i = 0; i < STREAMS_COUNT; ++i) {
			eventStreams.add(new MockEventStream());
		}

		((ManagedPreDestroy) manager).preDestroy();

		assertEquals(STREAMS_COUNT, eventStreams.size());
		for (MockEventStream eventStream : eventStreams) {
			assertEquals(1, eventStream.closeProbe);
		}
	}

	/** Pre-destroy should do nothing if sessions list is empty. */
	@Test
	public void preDestroy_EmptyStreams() throws Exception {
		ManagedPreDestroy manager = Classes.newInstance("js.net.EventStreamManagerImpl", factory);
		manager.preDestroy();
	}

	/** Subscribe should store configuration object to sessions map. */
	@Test
	public void subscribe() {
		EventStreamManager manager = Classes.newInstance("js.net.EventStreamManagerImpl", factory);
		EventStreamConfig config = new EventStreamConfig();

		String sessionID = manager.subscribe(config);
		assertNotNull(sessionID);

		Map<Object, EventStreamConfig> sessions = Classes.getFieldValue(manager, "sessions");
		assertEquals(1, sessions.size());
		Map.Entry<Object, EventStreamConfig> entry = sessions.entrySet().iterator().next();
		assertEquals("1", entry.getKey().toString());
		assertEquals(config, entry.getValue());
	}

	/**
	 * Create event stream should remove session ID from {@link EventStreamManagerImpl#sessions} and add newly created event
	 * stream to {@link EventStreamManagerImpl#eventStreams}.
	 */
	@Test
	public void createEventStream() {
		EventStreamManagerSPI manager = Classes.newInstance("js.net.EventStreamManagerImpl", factory);

		EventStreamConfig config = new EventStreamConfig();
		String sessionID = ((EventStreamManager) manager).subscribe(config);

		Map<Object, EventStreamConfig> sessions = Classes.getFieldValue(manager, "sessions");
		List<EventStream> eventStreams = Classes.getFieldValue(manager, "eventStreams");
		assertEquals(1, sessions.size());
		assertEquals(0, eventStreams.size());

		EventStream eventStream = manager.createEventStream(sessionID);
		assertNotNull(eventStream);
		assertEquals(0, sessions.size());
		assertEquals(1, eventStreams.size());
		assertEquals(eventStream, eventStreams.get(0));
	}

	/** Creating event stream with expired session ID should return null. */
	@Test
	public void createEventStream_StaleTimestamp() {
		Class<?> managerClass = Classes.forName("js.net.EventStreamManagerImpl");
		Classes.setFieldValue(managerClass, "SUBSCRIBE_TTL", 0);
		EventStreamManagerSPI manager = (EventStreamManagerSPI) Classes.newInstance(managerClass, factory);

		EventStreamConfig config = new EventStreamConfig();
		String sessionID = ((EventStreamManager) manager).subscribe(config);
		try {
			Thread.sleep(10);
		} catch (InterruptedException ignored) {
		}

		EventStream eventStream = manager.createEventStream(sessionID);
		assertNull(eventStream);
		Classes.setFieldValue(managerClass, "SUBSCRIBE_TTL", 10000);
	}

	@Test
	public void createEventStream_Concurrent() {
		final EventStreamManagerSPI manager = Classes.newInstance("js.net.EventStreamManagerImpl", factory);

		final List<String> sessionIDs = new ArrayList<>();
		for (int i = 0; i < 100; ++i) {
			EventStreamConfig config = new EventStreamConfig();
			sessionIDs.add(((EventStreamManager) manager).subscribe(config));
		}

		Thread[] threads = new Thread[sessionIDs.size()];
		for (int i = 0; i < threads.length; ++i) {
			final String sessionID = sessionIDs.get(i);
			threads[i] = new Thread() {
				public void run() {
					EventStream eventStream = manager.createEventStream(sessionID);
					assertNotNull(eventStream);
				}
			};
		}

		for (Thread thread : threads) {
			thread.run();
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void createEventStream_NullSessionID() {
		EventStreamManagerSPI manager = Classes.newInstance("js.net.EventStreamManagerImpl", factory);
		manager.createEventStream(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void createEventStream_EmptySessionID() {
		EventStreamManagerSPI manager = Classes.newInstance("js.net.EventStreamManagerImpl", factory);
		manager.createEventStream("");
	}

	@Test
	public void createEventStream_BadSessionID() {
		EventStreamManagerSPI manager = Classes.newInstance("js.net.EventStreamManagerImpl", factory);
		EventStream eventStream = manager.createEventStream("FAKE");
		assertNull(eventStream);
	}

	/** Destroy event stream should remove event stream from {@link EventStreamManagerImpl#eventStreams}. */
	@Test
	public void destroyEventStream() {
		EventStreamManagerSPI manager = Classes.newInstance("js.net.EventStreamManagerImpl", factory);

		List<EventStream> eventStreams = Classes.getFieldValue(manager, "eventStreams");
		assertEquals(0, eventStreams.size());

		EventStreamConfig config = new EventStreamConfig();
		String sessionID = ((EventStreamManager) manager).subscribe(config);

		EventStream eventStream = manager.createEventStream(sessionID);
		assertEquals(1, eventStreams.size());

		manager.destroyEventStream(eventStream);
		assertEquals(0, eventStreams.size());
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class MockAppFactory extends AppFactoryStub {
		@Override
		public <T> T getInstance(Class<? super T> interfaceClass, Object... args) {
			return (T) new MockEventStream();
		}
	}

	private static class MockEventStream extends EventStream {

	}
}
