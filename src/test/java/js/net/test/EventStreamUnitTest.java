package js.net.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import js.lang.BugError;
import js.lang.Event;
import js.net.EventStream;
import js.net.EventStreamConfig;
import js.unit.BlockingQueueStub;
import js.unit.JsonStub;
import js.util.Base64;
import js.util.Classes;

import org.junit.Before;
import org.junit.Test;

@SuppressWarnings({ "unused", "serial" })
public class EventStreamUnitTest {
	private MockBlockingQueue<Event> eventsQueue;
	private MockEventStream eventStream;

	@Before
	public void beforeTest() throws Exception {
		eventsQueue = new MockBlockingQueue<Event>();
		eventStream = new MockEventStream(eventsQueue);
	}

	@Test
	public void constructor() {
		eventStream = new MockEventStream();
		assertNotNull(Classes.getFieldValue(eventStream, EventStream.class, "json"));
		assertNotNull(Classes.getFieldValue(eventStream, EventStream.class, "eventsQueue"));
	}

	@Test
	public void config() {
		class MockSecretKey implements SecretKey {
			@Override
			public String getAlgorithm() {
				return "TST";
			}

			@Override
			public String getFormat() {
				return null;
			}

			@Override
			public byte[] getEncoded() {
				return new byte[] { 1 };
			}
		}
		SecretKey secretKey = new MockSecretKey();

		EventStreamConfig config = new EventStreamConfig();
		config.setSecretKey(secretKey);
		config.setKeepAlivePeriod(100);
		config.setParameter("name", "value");
		eventStream = new MockEventStream();
		eventStream.config(config);

		assertNotNull(Classes.getFieldValue(eventStream, EventStream.class, "secretKey"));
		assertEquals(100, Classes.getFieldValue(eventStream, EventStream.class, "keepAlivePeriod"));
		assertNotNull(Classes.getFieldValue(eventStream, EventStream.class, "parameters"));
		assertEquals("value", eventStream.getParameter("name", String.class));
	}

	/** Only first remote host setter takes effect. */
	@Test
	public void setRemoteHost() {
		Classes.setFieldValue(EventStream.class, "STREAM_ID", 0);
		
		eventStream = new MockEventStream();
		eventStream.setRemoteHost("localhost");
		assertEquals("#0:localhost", eventStream.toString());

		eventStream.setRemoteHost("fake");
		assertEquals("#0:localhost", eventStream.toString());
	}

	@Test
	public void push() {
		MockEvent event = new MockEvent(1);
		eventStream.push(event);
		assertEquals(1, eventsQueue.offerProbe);
	}

	@Test
	public void push_NoCapacity() {
		eventsQueue.noCapacity = true;
		MockEvent event = new MockEvent(1);
		eventStream.push(event);
		assertEquals(1, eventsQueue.offerProbe);
	}

	@Test
	public void push_Interrupted() {
		eventsQueue.exception = true;
		MockEvent event = new MockEvent(1);
		eventStream.push(event);
		assertEquals(1, eventsQueue.offerProbe);
		assertTrue(Thread.currentThread().isInterrupted());
	}

	@Test(expected = BugError.class)
	public void push_NotActive() {
		AtomicBoolean active = Classes.getFieldValue(eventStream, EventStream.class, "active");
		active.set(false);

		MockEvent event = new MockEvent(1);
		eventStream.push(event);
		assertEquals(0, eventsQueue.offerProbe);
	}

	@Test
	public void getParameter() throws Throwable {
		EventStreamConfig config = new EventStreamConfig();
		config.setParameter("integerValue", "19640315");
		eventStream.config(config);

		int integerValue = eventStream.getParameter("integerValue", Integer.class);
		assertEquals(19640315, integerValue);
	}

	@Test(expected = BugError.class)
	public void getParameter_NotConfigured() throws Throwable {
		eventStream.getParameter("integerValue", Integer.class);
	}

	@Test(expected = BugError.class)
	public void getParameter_Missing() throws Throwable {
		EventStreamConfig config = new EventStreamConfig();
		eventStream.config(config);
		eventStream.getParameter("integerValue", Integer.class);
	}

	@Test
	public void loop_NotActive() throws IOException {
		AtomicBoolean active = Classes.getFieldValue(eventStream, EventStream.class, "active");
		active.set(false);

		eventsQueue.exception = true;
		assertFalse(eventStream.loop());
	}

	@Test
	public void loop_Interrupted() throws IOException {
		eventsQueue.exception = true;
		assertTrue(eventStream.loop());
	}

	@Test
	public void loop_WriterError() throws IOException {
		StringWriter stringWriter = new StringWriter();
		eventStream.setWriter(new MockPrintWriter(stringWriter));
		assertFalse(eventStream.loop());
	}

	@Test
	public void loop_sendEvent() throws Throwable {
		StringWriter stringWriter = new StringWriter();
		eventStream.setWriter(new PrintWriter(stringWriter));
		eventStream.push(new MockEvent(19640315));

		assertTrue(eventStream.loop());
		assertEquals("data:{\"class\":\"js.net.test.EventStreamUnitTest$MockEvent\",\"id\":19640315}\r\n\r\n", stringWriter.toString());
	}

	@Test
	public void loop_sendEvent_WriterError() throws Throwable {
		StringWriter stringWriter = new StringWriter();
		eventStream.setWriter(new MockPrintWriter(stringWriter));

		MockEvent event = new MockEvent(19640315);
		eventStream.push(event);

		assertFalse(eventStream.loop());
		assertEquals("data:{\"class\":\"js.net.test.EventStreamUnitTest$MockEvent\",\"id\":19640315}\r\n\r\n", stringWriter.toString());
	}

	@Test
	public void loop_sendKeepAlive() throws Throwable {
		EventStreamConfig config = new EventStreamConfig();
		config.setKeepAlivePeriod(1);
		eventStream.config(config);

		StringWriter stringWriter = new StringWriter();
		eventStream.setWriter(new PrintWriter(stringWriter));

		eventStream.loop();
		assertEquals("data:\r\n\r\n", stringWriter.toString());
	}

	@Test
	public void loop_ShutdownEvent() throws Throwable {
		StringWriter stringWriter = new StringWriter();
		eventStream.setWriter(new PrintWriter(stringWriter));
		eventStream.push((Event) Classes.newInstance("js.net.EventStream$ShutdownEvent"));

		assertFalse(eventStream.loop());
	}

	@Test
	public void sendEvent() throws Throwable {
		StringWriter stringWriter = new StringWriter();
		eventStream.setWriter(new PrintWriter(stringWriter));
		eventStream.sendEvent(new MockEvent(19640315));
		assertEquals("data:{\"class\":\"js.net.test.EventStreamUnitTest$MockEvent\",\"id\":19640315}\r\n\r\n", stringWriter.toString());
	}

	/** Send event does not perform print writer check error. */
	@Test
	public void sendEvent_WriterError() throws Throwable {
		StringWriter stringWriter = new StringWriter();
		eventStream.setWriter(new MockPrintWriter(stringWriter));
		eventStream.sendEvent(new MockEvent(19640315));
		assertEquals("data:{\"class\":\"js.net.test.EventStreamUnitTest$MockEvent\",\"id\":19640315}\r\n\r\n", stringWriter.toString());
	}

	@Test
	public void sendEvent_SecureChannel() throws Throwable {
		KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
		keyGenerator.init(128);
		SecretKey secretKey = keyGenerator.generateKey();

		EventStreamConfig config = new EventStreamConfig();
		config.setSecretKey(secretKey);

		StringWriter stringWriter = new StringWriter();
		eventStream.config(config);
		eventStream.setWriter(new PrintWriter(stringWriter));
		eventStream.sendEvent(new MockEvent(19640315));

		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, secretKey);
		String event = stringWriter.toString();
		byte[] message = cipher.doFinal(Base64.decode(event.substring(5, event.length() - 4)));

		assertEquals("{\"class\":\"js.net.test.EventStreamUnitTest$MockEvent\",\"id\":19640315}", new String(message));
	}

	@Test
	public void sendKeepAlive() throws Throwable {
		StringWriter stringWriter = new StringWriter();
		eventStream.setWriter(new PrintWriter(stringWriter));

		eventStream.sendKeepAlive();
		assertEquals("data:\r\n\r\n", stringWriter.toString());
	}

	@Test
	public void close() {
		AtomicBoolean active = Classes.getFieldValue(eventStream, EventStream.class, "active");
		assertTrue(active.get());
		eventStream.close();
		assertFalse(active.get());
		eventStream.close();
		assertFalse(active.get());
	}

	@Test(expected = BugError.class)
	public void jsonSerializer_PrinterException() throws Exception {
		class MockJson extends JsonStub {
			@Override
			public void stringifyObject(Writer writer, Object value) throws IOException {
				throw new IOException();
			}
		}
		Object json = Classes.newInstance("js.net.EventStream$JsonSerializer", new MockJson());
		Classes.invoke(json, "serialize", new PrintWriter(new StringWriter()), new MockEvent(1));
	}

	@Test(expected = BugError.class)
	public void jsonSerializer_StringException() throws Exception {
		class MockJson extends JsonStub {
			@Override
			public void stringifyObject(Writer writer, Object value) throws IOException {
				throw new IOException();
			}
		}
		Object json = Classes.newInstance("js.net.EventStream$JsonSerializer", new MockJson());
		Classes.invoke(json, "serialize", new MockEvent(1));
	}

	// --------------------------------------------------------------------------------------------
	// FIXTURE

	private static class MockEvent implements Event {
		int id;

		public MockEvent(int id) {
			this.id = id;
		}
	}

	private static class MockEventStream extends EventStream {
		public MockEventStream() {
			super();
		}

		public MockEventStream(BlockingQueue<Event> eventsQueue) {
			super();
			Classes.setFieldValue(this, EventStream.class, "eventsQueue", eventsQueue);
		}

		@Override
		public void config(EventStreamConfig config) {
			super.config(config);
		}

		@Override
		public void setRemoteHost(String remoteHost) {
			super.setRemoteHost(remoteHost);
		}

		@Override
		public void setWriter(PrintWriter writer) {
			super.setWriter(writer);
		}

		@Override
		public <T> T getParameter(String name, Class<T> type) {
			return super.getParameter(name, type);
		}

		@Override
		public boolean loop() {
			return super.loop();
		}

		@Override
		public void sendEvent(Event event) {
			super.sendEvent(event);
		}

		@Override
		public void sendKeepAlive() {
			super.sendKeepAlive();
		}
	}

	private static class MockBlockingQueue<E> extends BlockingQueueStub<E> {
		private boolean noCapacity;
		private boolean exception;
		private E e;

		private int offerProbe;
		private int pollProbe;

		@Override
		public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
			++offerProbe;
			if (noCapacity) {
				return false;
			}
			if (exception) {
				throw new InterruptedException();
			}
			this.e = e;
			return true;
		}

		@Override
		public E poll(long timeout, TimeUnit unit) throws InterruptedException {
			++pollProbe;
			if (exception) {
				throw new InterruptedException();
			}
			return e;
		}
	}

	private static class MockPrintWriter extends PrintWriter {
		public MockPrintWriter(Writer out) {
			super(out);
		}

		@Override
		public boolean checkError() {
			return true;
		}
	}
}
