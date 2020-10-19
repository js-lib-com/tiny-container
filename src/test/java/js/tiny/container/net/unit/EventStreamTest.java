package js.tiny.container.net.unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;

import js.json.Json;
import js.lang.BugError;
import js.lang.Event;
import js.tiny.container.annotation.TestConstructor;
import js.tiny.container.net.EventStream;
import js.tiny.container.net.EventStreamConfig;
import js.util.Classes;

@RunWith(MockitoJUnitRunner.class)
public class EventStreamTest {
	@Mock
	private Json json;
	@Mock
	private BlockingQueue<Event> eventsQueue;
	@Mock
	private PrintWriter writer;
	@Mock
	EventStreamConfig config;
	@Mock
	SecretKey key;

	private EventStream eventStream;

	@Before
	public void beforeTest() {
		eventStream = new EventStream(json, eventsQueue, true);
	}

	/** Assert test constructor is marked with {@link TestConstructor} annotation. */
	@Test
	public void testConstructor() throws NoSuchMethodException, SecurityException {
		Constructor<EventStream> testConstructor = EventStream.class.getConstructor(Json.class, BlockingQueue.class, boolean.class);
		assertNotNull("Missing annotation from test constructor.", testConstructor.getAnnotation(TestConstructor.class));
	}

	@Test
	public void config() {
		when(config.hasKeepAlivePeriod()).thenReturn(true);
		when(config.getKeepAlivePeriod()).thenReturn(30000);
		eventStream.config(config);
		assertThat(keepAlivePeriod(), equalTo(30000));
	}

	/** Conformity test for event push. */
	@Test
	public void push() throws InterruptedException {
		Event event = new TestEvent("Send event.");
		when(eventsQueue.offer(event, 4000, TimeUnit.MILLISECONDS)).thenReturn(true);

		eventStream.push(event);

		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		ArgumentCaptor<Long> timeoutCaptor = ArgumentCaptor.forClass(Long.class);
		ArgumentCaptor<TimeUnit> unitCaptor = ArgumentCaptor.forClass(TimeUnit.class);
		verify(eventsQueue).offer(eventCaptor.capture(), timeoutCaptor.capture(), unitCaptor.capture());

		assertThat(eventCaptor.getValue(), equalTo(event));
		assertThat(timeoutCaptor.getValue(), equalTo(4000L));
		assertThat(unitCaptor.getValue(), equalTo(TimeUnit.MILLISECONDS));
	}

	/** On events queue timeout there is no error but event is lost with record on application logger. */
	@Test
	public void push_Timeout() throws InterruptedException {
		// events queue mock returns false by default on 'offer' method signaling timeout
		eventStream.push(null);
	}

	/** Events queue interruption exception is recorded to application logger and current thread interrupted. */
	@Test
	public void push_Interrupted() throws InterruptedException {
		when(eventsQueue.offer((Event) isNull(), anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException());
		eventStream.push(null);
		assertTrue(Thread.currentThread().isInterrupted());
	}

	/** It is considered a bug attempting to push on a closed event stream. */
	@Test(expected = BugError.class)
	public void push_NotActive() throws InterruptedException {
		eventStream = new EventStream(json, eventsQueue, false);
		eventStream.push(null);
	}

	/** Loop conformity test. It gets an event for events queue and end it to writer. */
	@Test
	public void loop_sendEvent() throws Throwable {
		Event event = new TestEvent("Send event.");
		doWrite("{\"text\":\"Send event.\"}").when(json).stringify(any(Writer.class), eq(event));
		// 40000 is the default keep alive period
		// uses fixed values in order to check that #keepAlivePeriod is indeed used
		when(eventsQueue.poll(40000, TimeUnit.MILLISECONDS)).thenReturn(event);

		StringWriter buffer = new StringWriter();
		eventStream.setWriter(new PrintWriter(buffer));
		eventStream.push(event);

		assertTrue(eventStream.loop());
		assertThat(buffer.toString(), equalTo("event:TestEvent\r\ndata:{\"text\":\"Send event.\"}\r\n\r\n"));
	}

	/** If poll timeout expires, events queue returns null in which case loop sends keep alive event. */
	@Test
	public void loop_sendKeepAlive() throws Throwable {
		StringWriter buffer = new StringWriter();
		eventStream.setWriter(new PrintWriter(buffer));
		eventStream.push(null);

		assertTrue(eventStream.loop());
		assertThat(buffer.toString(), equalTo("event:KeepAliveEvent\r\ndata:\r\n\r\n"));
	}

	/** On events queue poll interruption continue loop, if event stream is still active. */
	@Test
	public void loop_Interrupted_Active() throws InterruptedException {
		when(eventsQueue.poll(anyLong(), any(TimeUnit.class))).thenThrow(InterruptedException.class);
		assertTrue(eventStream.loop());
	}

	/** On events queue poll interruption breaks the loop, if event stream is not active anymore. */
	@Test
	public void loop_Interrupted_NotActive() throws InterruptedException {
		eventStream = new EventStream(json, eventsQueue, false);
		when(eventsQueue.poll(anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException());
		assertFalse(eventStream.loop());
	}

	/** Breaks the loop when print writer check error returns true. */
	@Test
	public void loop_WriteError() {
		when(writer.checkError()).thenReturn(true);
		eventStream.setWriter(writer);
		assertFalse(eventStream.loop());
	}

	/** Breaks the loop when events queue returns shutdown event. */
	@Test
	public void loop_ShutdownEvent() throws InterruptedException {
		Event event = Classes.newInstance("js.tiny.container.net.EventStream$ShutdownEvent");
		when(eventsQueue.poll(anyLong(), any(TimeUnit.class))).thenReturn(event);
		assertFalse(eventStream.loop());
	}

	@Test
	public void sendKeepAlive() throws Exception {
		StringWriter buffer = new StringWriter();
		eventStream.setWriter(new PrintWriter(buffer));
		Classes.invoke(eventStream, "sendKeepAlive");
		assertThat(buffer.toString(), equalTo("event:KeepAliveEvent\r\ndata:\r\n\r\n"));
	}

	@Test
	public void sendEvent() throws Exception {
		Event event = new TestEvent("Send event.");
		doWrite("{\"text\":\"Send event.\"}").when(json).stringify(any(Writer.class), eq(event));
		StringWriter buffer = new StringWriter();
		eventStream.setWriter(new PrintWriter(buffer));
		Classes.invoke(eventStream, "sendEvent", event);
		assertThat(buffer.toString(), equalTo("event:TestEvent\r\ndata:{\"text\":\"Send event.\"}\r\n\r\n"));
	}

	@Test
	public void close() throws InterruptedException {
		eventStream.close();
		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		verify(eventsQueue).offer(eventCaptor.capture(), anyLong(), any(TimeUnit.class));

		Class<? extends Event> eventClass = Classes.forName("js.tiny.container.net.EventStream$ShutdownEvent");
		assertThat(eventCaptor.getValue(), instanceOf(eventClass));
	}

	// --------------------------------------------------------------------------------------------

	private int keepAlivePeriod() {
		return Classes.getFieldValue(eventStream, EventStream.class, "keepAlivePeriod");
	}

	private static Stubber doWrite(final String json) {
		return doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Writer writer = invocation.getArgument(0);
				writer.write(json);
				return null;
			}
		});
	}

	private static class TestEvent implements Event {
		@SuppressWarnings("unused")
		private final String text;

		public TestEvent(String text) {
			this.text = text;
		}
	}
}
