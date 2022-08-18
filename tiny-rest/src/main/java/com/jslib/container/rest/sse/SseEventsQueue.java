package com.jslib.container.rest.sse;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;

import jakarta.ws.rs.sse.OutboundSseEvent;

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
				log.warn("Timeout trying to push event on events queue. Event |{event}| not processed.", event);
			}
		} catch (InterruptedException unused) {
			log.warn("Thread interruption on event queue |{event_queue}| while trying to push event. Event |{event}| not processed.", this, event);
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
