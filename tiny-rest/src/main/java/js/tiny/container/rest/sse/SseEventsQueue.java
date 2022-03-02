package js.tiny.container.rest.sse;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.sse.OutboundSseEvent;
import js.log.Log;
import js.log.LogFactory;

public class SseEventsQueue {
	private static final Log log = LogFactory.getLog(SseEventsQueue.class);

	private static final int EVENTS_QUEUE_PUSH_TIMEOUT = 4000;

	private final BlockingQueue<OutboundSseEvent> eventsQueue;

	public SseEventsQueue() {
		log.trace("SseEventsQueue()");
		this.eventsQueue = new LinkedBlockingQueue<>();
	}

	public void offer(OutboundSseEvent event) {
		log.trace("offer(OutboundSseEvent)");
		try {
			if (!eventsQueue.offer(event, EVENTS_QUEUE_PUSH_TIMEOUT, TimeUnit.MILLISECONDS)) {
				log.warn("Timeout trying to push event on events queue. Event |%s| not processed.", event);
			}
		} catch (InterruptedException unused) {
			log.warn("Thread interruption on event stream |%s| while trying to push event to queue. Event |%s| not processed.", this, event);
			Thread.currentThread().interrupt();
		}
	}

	public OutboundSseEvent poll(long timeout) {
		log.trace("poll()");
		for (;;) {
			try {
				return eventsQueue.poll(timeout, TimeUnit.MILLISECONDS);
			} catch (InterruptedException unused) {
				continue;
			}
		}
	}
}
