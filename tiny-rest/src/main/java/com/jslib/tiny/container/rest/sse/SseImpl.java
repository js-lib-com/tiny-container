package com.jslib.tiny.container.rest.sse;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;

import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;

public class SseImpl implements Sse {
	private static final Log log = LogFactory.getLog(SseImpl.class);

	public SseImpl() {
		log.trace("SseImpl()");
	}

	@Override
	public OutboundSseEvent.Builder newEventBuilder() {
		return OutboundSseEventImpl.builder();
	}

	@Override
	public SseBroadcaster newBroadcaster() {
		return new SseBroadcasterImpl();
	}
}
