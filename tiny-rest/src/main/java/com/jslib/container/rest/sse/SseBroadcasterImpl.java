package com.jslib.container.rest.sse;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;

import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;
import com.jslib.lang.KeepAliveEvent;

class SseBroadcasterImpl implements SseBroadcaster, Runnable {
	private static final Log log = LogFactory.getLog(SseBroadcasterImpl.class);

	/**
	 * Send keep alive if there are no events sent for this amount of time, in milliseconds. Keep alive events have double role:
	 * <ol>
	 * <li>Allows for both client and this server side event stream to know that peer is up,
	 * <li>Avoid routers with idle timeout to drop connections when no events are sent.
	 * </ol>
	 * <p>
	 * For second case this timeout value is critical. If is larger than router idle connection timeout, connection will be lost
	 * and client will reconnect periodically; in this case events stream behaves like a polling mechanism with resources waste
	 * and bad responsiveness.
	 * <p>
	 * Routers idle connection timeout is manufacturer specific and / or network administrator customizable and can vary
	 * greatly, including complete disabling. In fact it is a not standard behavior. TCP/IP does not require such timeout; it is
	 * merely a technological limitation used to save routers internal resources. Anyway, from practice is observed that the
	 * lower idle connection timeout value found is one minute. This event stream class uses 40 seconds in order to cope with
	 * delays generated by server heavy loading and network latency. It is a heuristic / brute force approach. A better solution
	 * may be an adaptive timeout that client may learn and communicate to server when opens events stream.
	 * <p>
	 * Now, some considerations about keep alive packages bandwidth consumption. Keep alive event data length is about 40 bytes.
	 * With current 40 seconds period results in exactly one byte per second, a perfectly acceptable overload. And even if
	 * include TCP/IP headers and acknowledge packet bandwidth consumption will not exceed 3 B/s.
	 */
	private static final int KEEP_ALIVE_TIMEOUT = 40000;

	private final Queue<SseEventSinkImpl> sinksQueue;

	private final SseEventsQueue eventsQueue;

	private final Thread thread;

	private final AtomicBoolean closed;

	private Consumer<SseEventSink> onEventSinkClose;
	private BiConsumer<SseEventSink, Throwable> onEventSinkError;

	public SseBroadcasterImpl() {
		this.sinksQueue = new ConcurrentLinkedQueue<>();
		this.eventsQueue = new SseEventsQueue();

		this.thread = new Thread(this, "SSE Broadcaster");
		this.thread.setDaemon(true);
		this.thread.start();

		this.closed = new AtomicBoolean(false);
	}

	@Override
	public void onClose(Consumer<SseEventSink> onEventSinkClose) {
		this.onEventSinkClose = onEventSinkClose;
	}

	@Override
	public void onError(BiConsumer<SseEventSink, Throwable> onEventSinkError) {
		this.onEventSinkError = onEventSinkError;
	}

	@Override
	public void register(SseEventSink eventSink) {
		log.debug("Add event sink |{event_sink}| to broadcaster queue.", eventSink);
		if (closed.get()) {
			throw new IllegalStateException("Attempt to register event sink after SSE broadcaster close.");
		}

		SseEventSinkImpl eventSinkImpl = (SseEventSinkImpl) eventSink;
		sinksQueue.add(eventSinkImpl);
		eventSinkImpl.setOnEventSinkClose(onEventSinkClose);
		eventSinkImpl.setOnEventSinkError(onEventSinkError);
	}

	@Override
	public CompletionStage<?> broadcast(OutboundSseEvent event) {
		if (closed.get()) {
			throw new IllegalStateException("Attempt to send outbound event after SSE broadcaster close.");
		}
		// do not bother to push event on events queue if there are no sink subscribed
		// sinks queue is thread-safe
		if (!sinksQueue.isEmpty()) {
			eventsQueue.offer(event);
		}
		return CompletableFuture.completedFuture(null);
	}

	/**
	 * Close all registered event sinks and complete (finish) related servlet asynchronous contexts, then shutdown processing
	 * loop. Since broadcaster life cycle is controlled by application, this method should be explicitly invoked by application
	 * logic.
	 */
	@Override
	public void close() {
		if (!closed.getAndSet(true)) {
			sinksQueue.forEach(sink -> sink.close(true));
			eventsQueue.offer(SseEventFactory.get(new ShutdownEvent()));
		}
	}

	/**
	 * Broadcaster processing loop sends events from {@link #eventsQueue} to all registered event sinks, see
	 * {@link #sinksQueue}. Processing loop keeps running till got {@link ShutdownEvent} sent by {@link #close()} method.
	 * 
	 * Processing loop waits for an outbound SSE event for {@link #KEEP_ALIVE_TIMEOUT} time period; if no event available on
	 * events queue sends placeholder event, {@link KeepAliveEvent}.
	 * 
	 * Events sending logic actually delegates every event sink from registered sinks queue. If there is exception while sending
	 * the event, most probably due to SSE client socket close, event sink is closed - see {@link SseEventSinkImpl#close()}, and
	 * removed from sinks queue.
	 */
	@Override
	public void run() {
		log.debug("Start broadcaster processing loop.");
		long startTimeMillis = System.currentTimeMillis();

		for (;;) {
			OutboundSseEvent event = eventsQueue.poll(KEEP_ALIVE_TIMEOUT);
			if (event == null) {
				event = OutboundSseEventImpl.builder().comment("keepalive").build();
			}

			if (event.getData() instanceof ShutdownEvent) {
				log.debug("Got shutdown event. Break event stream loop.");
				break;
			}

			List<SseEventSinkImpl> closedSinks = new ArrayList<>();
			for (SseEventSinkImpl sink : sinksQueue) {
				sink.send(event).thenAccept(value -> {
					if (!((Boolean) value)) {
						log.debug("Send fail on event sink |{event_sink}|; most probably SSE client close. Remove event sink from broadcaster queue.", sink);
						closedSinks.add(sink);
					}
				});
			}

			closedSinks.forEach(sink -> {
				sinksQueue.remove(sink);
				// do not ask event sink close method to complete (finish) related asynchronous context; above event sink send
				// exception is detected by asynchronous context that completes itself
				sink.close(false);
			});
		}

		log.debug("End broadcaster processing loop. Active for {active_time} sec.", (System.currentTimeMillis() - startTimeMillis) / 1000.0);
	}

	/**
	 * Internal event used to shutdown events stream. It is pushed on events queue by {@link EventStream#close()}; as a result
	 * events stream {@link EventStream#loop() loop} is stopped and {@link EventStreamServlet} is released.
	 * 
	 * @author Iulian Rotaru
	 */
	private static final class ShutdownEvent {
	}
}
