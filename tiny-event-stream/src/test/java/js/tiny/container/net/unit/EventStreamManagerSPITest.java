package js.tiny.container.net.unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.tiny.container.net.EventGuest;
import js.tiny.container.net.EventStream;
import js.tiny.container.net.EventStreamConfig;
import js.tiny.container.net.EventStreamManager;
import js.tiny.container.net.EventStreamManagerImpl;
import js.tiny.container.spi.IContainer;

@RunWith(MockitoJUnitRunner.class)
public class EventStreamManagerSPITest {
	@Mock
	private IContainer container;
	@Mock
	private Map<Principal, EventStream> eventsMap;
	@Mock
	private List<EventStream> eventsList;
	@Mock
	private EventStream eventStream;
	@Mock
	private EventStreamConfig config;

	private EventStreamManager manager;

	@Before
	public void beforeTest() {
		manager = new EventStreamManagerImpl(container, eventsMap);
	}

	@Test
	public void createEventStream() {
		when(container.getInstance(EventStream.class)).thenReturn(eventStream);
		manager.createEventStream(null, null);

		// configuration object is not provided
		verify(eventStream, times(0)).config(any(EventStreamConfig.class));

		ArgumentCaptor<Principal> principalCaptor = ArgumentCaptor.forClass(Principal.class);
		verify(eventsMap, times(1)).put(principalCaptor.capture(), eq(eventStream));

		assertThat(principalCaptor.getValue(), notNullValue());
		assertThat(principalCaptor.getValue(), instanceOf(EventGuest.class));
	}

	@Test
	public void createEventStream_Principal() {
		class User implements Principal {
			@Override
			public String getName() {
				return null;
			}
		}

		when(container.getInstance(EventStream.class)).thenReturn(eventStream);
		manager.createEventStream(new User(), null);

		verify(eventStream, times(0)).config(any(EventStreamConfig.class));

		ArgumentCaptor<Principal> principalCaptor = ArgumentCaptor.forClass(Principal.class);
		verify(eventsMap, times(1)).put(principalCaptor.capture(), eq(eventStream));

		assertThat(principalCaptor.getValue(), notNullValue());
		assertThat(principalCaptor.getValue(), instanceOf(User.class));
	}

	@Test
	public void createEventStream_Config() {
		when(container.getInstance(EventStream.class)).thenReturn(eventStream);
		manager.createEventStream(null, config);

		// configuration object is passed to created event stream
		verify(eventStream, times(1)).config(any(EventStreamConfig.class));

		ArgumentCaptor<Principal> principalCaptor = ArgumentCaptor.forClass(Principal.class);
		verify(eventsMap, times(1)).put(principalCaptor.capture(), eq(eventStream));

		assertThat(principalCaptor.getValue(), notNullValue());
		assertThat(principalCaptor.getValue(), instanceOf(EventGuest.class));
	}

	@Test
	public void destroyEventStream() {
		when(eventsMap.values()).thenReturn(eventsList);
		manager.destroyEventStream(eventStream);
		verify(eventsList, times(1)).remove(eventStream);
	}

	@Test
	public void preDestroy() {
		when(eventsMap.size()).thenReturn(1);
		when(eventsMap.values()).thenReturn(Arrays.asList(eventStream));
		((EventStreamManagerImpl) manager).preDestroy();
		verify(eventStream, times(1)).close();
	}
}
