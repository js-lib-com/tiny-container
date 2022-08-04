package js.tiny.container.rest.sse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;

import jakarta.inject.Inject;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;
import js.json.Json;
import js.lang.BugError;

public class SseEventSinkImpl implements SseEventSink, AsyncListener {
	private static final Log log = LogFactory.getLog(SseEventSinkImpl.class);

	/**
	 * Every event sink has its own unique (incremental) ID used merely for debugging. Event sink ID is unique only on current
	 * application run; it is not preserved after application restart.
	 */
	private static int SINK_ID;

	private final long startTimeMillis;

	private final AtomicBoolean closed;

	/** Event sink string representation. */
	private final String string;

	/** Events JSON serializer specialized for print writer that does not throws IO exceptions. */
	private final JsonSerializer json;

	/** Servlet asynchronous context. */
	private AsyncContext asyncContext;

	/**
	 * Optional listener for event sink close, default to null. Fire by {@link #close()}, no matter if invoked on by
	 * asynchronous context on its completion or by application logic. Registered by application logic via
	 * {@link SseBroadcaster#onClose(Consumer)}.
	 */
	private Consumer<SseEventSink> onEventSinkClose;

	/**
	 * Optional listener for event sink exceptions, default to null. Fired by {@link #onError(AsyncEvent)} when asynchronous
	 * context detects an exception. Registered by application logic via {@link SseBroadcaster#onError(BiConsumer)}.
	 */
	private BiConsumer<SseEventSink, Throwable> onEventSinkError;

	/** Print writer for asynchronous context response. Used to convey JSON serialized events to SSE client. */
	private PrintWriter writer;

	@Inject
	public SseEventSinkImpl(Json json) {
		log.trace("SseEventSinkImpl(Json)");
		this.startTimeMillis = System.currentTimeMillis();
		this.closed = new AtomicBoolean(false);
		this.string = "#" + SINK_ID++;
		this.json = new JsonSerializer(json);
	}

	public void setAsyncContext(AsyncContext asyncContext) {
		this.asyncContext = asyncContext;
		this.asyncContext.addListener(this);
	}

	public void setOnEventSinkClose(Consumer<SseEventSink> onEventSinkClose) {
		this.onEventSinkClose = onEventSinkClose;
	}

	public void setOnEventSinkError(BiConsumer<SseEventSink, Throwable> onEventSinkError) {
		this.onEventSinkError = onEventSinkError;
	}

	public void setWriter(PrintWriter writer) {
		this.writer = writer;
	}

	@Override
	public CompletionStage<Boolean> send(OutboundSseEvent event) {
		if (closed.get()) {
			throw new IllegalStateException("Attempt to send SSE event to closed event sink: " + toString());
		}
		log.trace("Send event |%s| to event sink |%s|.", event, this);

		CompletableFuture<Boolean> future = new CompletableFuture<>();
		int fieldsCount = 0;

		if (event.getName() != null) {
			writer.write("event:");
			writer.write(event.getName());
			writer.write("\r\n");
			++fieldsCount;
		}

		if (event.getId() != null) {
			writer.write("id:");
			writer.write(event.getId());
			writer.write("\r\n");
			++fieldsCount;
		}

		if (event.getData() != null) {
			writer.write("data:");
			json.serialize(writer, event.getData());
			writer.write("\r\n");
			++fieldsCount;
		}

		if (event.isReconnectDelaySet()) {
			writer.write("retry:");
			writer.write(Long.toString(event.getReconnectDelay()));
			writer.write("\r\n");
			++fieldsCount;
		}

		if (event.getComment() != null) {
			writer.write(":");
			writer.write(event.getComment());
			writer.write("\r\n");
			++fieldsCount;
		}

		if (fieldsCount > 0) {
			// single end of line is the mark for event end
			writer.write("\r\n");
			// check error returns true if there is an error while trying to flush the writer to underlying socket
			future.complete(!writer.checkError());
		} else {
			future.complete(true);
		}

		return future;
	}

	/**
	 * Application logic can call this method to close this event sink and finish related asynchronous context. It delegates
	 * {@link #close(boolean)} with <code>closeAsyncContext</code> argument set to true.
	 */
	@Override
	public void close() {
		close(true);
	}

	/**
	 * Close response print writer and, if required, mark asynchronous context complete then invoke event sink close listener,
	 * if set. This method is guarded by {@link #closed} atomic flag; it can be invoked multiple time and is thread safe.
	 * 
	 * This method is invoked by events broadcaster processing loop, see {@link SseBroadcasterImpl#run()}, when detects client
	 * socket close. In this case <code>closeAsyncContext</code> argument is false because asynchronous context detects IO
	 * exception, generated by client socket close, and complete itself.
	 * 
	 * Also, {@link SseBroadcasterImpl#close()} invoke this method when broadcaster is closed. In this case
	 * <code>closeAsyncContext</code> argument is true and asynchronous context is explicitly completed (finished).
	 * 
	 * In theory, it can also be invoked by application logic via {@link #close()} interface, with
	 * <code>closeAsyncContext</code> argument set to true.
	 * 
	 * Attempting to {@link #send(OutboundSseEvent)} any event after close will throw illegal state.
	 * 
	 * @param closeAsyncContext flag true if need to mark asynchronous context complete.
	 */
	public void close(boolean closeAsyncContext) {
		if (!closed.getAndSet(true)) {
			log.debug("Close response print writer on event sink |%s|.", this);
			writer.close();

			if (closeAsyncContext) {
				log.debug("Mark asynchronous context complete for event sink |%s|.", this);
				asyncContext.complete();
			}

			if (onEventSinkClose != null) {
				onEventSinkClose.accept(this);
			}
		}
	}

	@Override
	public boolean isClosed() {
		return closed.get();
	}

	@Override
	public String toString() {
		return string;
	}

	// --------------------------------------------------------------------------------------------
	// asynchronous context life cycle listeners

	@Override
	public void onStartAsync(AsyncEvent event) throws IOException {
		log.debug("Event sink |%s| restarted.", this);
	}

	@Override
	public void onComplete(AsyncEvent event) throws IOException {
		log.info("Event sink |%s| complete. Active for %.2f sec.", this, (System.currentTimeMillis() - startTimeMillis) / 1000.0);
		// do not ask event sink close method to complete (finish) related asynchronous context; it is already completed
		close(false);
	}

	@Override
	public void onTimeout(AsyncEvent event) throws IOException {
		log.error("Timeout on event sink |%s|.", this);
	}

	@Override
	public void onError(AsyncEvent event) throws IOException {
		if (event.getThrowable() instanceof IOException) {
			log.debug("IO exeption on event sink |%s|; most probably SSE client close.", this);
		} else {
			log.dump(String.format("Error on event sink |%s|:", this), event.getThrowable());
		}
		if (onEventSinkError != null) {
			onEventSinkError.accept(this, event.getThrowable());
		}
	}

	// --------------------------------------------------------------------------------------------

	/**
	 * JSON serializer for event objects. Thin wrapper for {@link Json} service used with print writer that does not generate IO
	 * exceptions.
	 * 
	 * @author Iulian Rotaru
	 */
	private static final class JsonSerializer {
		/** JSON service implementation. */
		private final Json json;

		/**
		 * Create JSON serializer instance.
		 * 
		 * @param json JSON service implementation.
		 */
		public JsonSerializer(Json json) {
			this.json = json;
		}

		/**
		 * Serialize event as JSON stream to given writer. Since writer is a {@link PrintWriter} it does not throw
		 * {@link IOException}. As a consequence this method does not throw exception too.
		 * 
		 * @param writer print writer,
		 * @param event event object.
		 */
		public void serialize(PrintWriter writer, Object event) {
			try {
				json.stringify(writer, event);
			} catch (IOException e) {
				// we never step here since print writer never throws IO exceptions
				throw new BugError(e);
			}
		}
	}
}
