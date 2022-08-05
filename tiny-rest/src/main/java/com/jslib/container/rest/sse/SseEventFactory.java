package com.jslib.container.rest.sse;

import jakarta.ws.rs.sse.OutboundSseEvent;

public class SseEventFactory {
	public static OutboundSseEvent get(Object event) {
		return OutboundSseEventImpl.builder().name(event.getClass().getSimpleName()).data(event).build();
	}
}
