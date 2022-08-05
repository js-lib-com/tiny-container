package com.jslib.container.rest.sse;

import java.lang.reflect.Type;

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import com.jslib.util.Params;
import com.jslib.util.Strings;

public class OutboundSseEventImpl implements OutboundSseEvent {
	private String id;
	private String name;
	private Object data;
	private long reconnectDelay;
	private boolean reconnectDelaySet;
	private String comment;

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getComment() {
		return comment;
	}

	@Override
	public long getReconnectDelay() {
		return reconnectDelay;
	}

	@Override
	public boolean isReconnectDelaySet() {
		return reconnectDelaySet;
	}

	@Override
	public Class<?> getType() {
		return data != null ? data.getClass() : null;
	}

	@Override
	public Type getGenericType() {
		return data != null ? data.getClass() : null;
	}

	@Override
	public MediaType getMediaType() {
		return null;
	}

	@Override
	public Object getData() {
		return data;
	}

	@Override
	public String toString() {
		return comment == null ? Strings.toString(name, id) : comment;
	}

	// --------------------------------------------------------------------------------------------

	public static OutboundSseEvent.Builder builder() {
		return new Builder();
	}

	private static class Builder implements OutboundSseEvent.Builder {
		private final OutboundSseEventImpl event;

		public Builder() {
			this.event = new OutboundSseEventImpl();
		}

		@Override
		public Builder id(String id) {
			Params.notNullOrEmpty(id, "ID");
			event.id = id;
			return this;
		}

		@Override
		public Builder name(String name) {
			Params.notNullOrEmpty(name, "Name");
			event.name = name;
			return this;
		}

		@Override
		public Builder reconnectDelay(long reconnectDelay) {
			Params.positive(reconnectDelay, "Reconnect delay");
			event.reconnectDelay = reconnectDelay;
			event.reconnectDelaySet = true;
			return this;
		}

		@Override
		public Builder mediaType(MediaType mediaType) {
			if (!mediaType.getType().equals("application") || !mediaType.getSubtype().equals("json")) {
				throw new UnsupportedOperationException("Current implementation supports only JSON media type.");
			}
			return this;
		}

		@Override
		public Builder comment(String comment) {
			Params.notNullOrEmpty(comment, "Comment");
			event.comment = comment;
			return this;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Builder data(Class type, Object data) {
			event.data = data;
			return this;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Builder data(GenericType type, Object data) {
			throw new UnsupportedOperationException("OutboundSseEventBuilderImpl#data(GenericType type, Object data)");
		}

		@Override
		public Builder data(Object data) {
			event.data = data;
			return this;
		}

		@Override
		public OutboundSseEvent build() {
			return event;
		}
	}
}
