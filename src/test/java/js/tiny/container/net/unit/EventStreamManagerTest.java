package js.tiny.container.net.unit;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.Event;
import js.tiny.container.annotation.TestConstructor;
import js.tiny.container.core.AppFactory;
import js.tiny.container.net.EventStream;
import js.tiny.container.net.EventStreamConfig;
import js.tiny.container.net.EventStreamManager;
import js.tiny.container.net.EventStreamManagerImpl;

@RunWith(MockitoJUnitRunner.class)
public class EventStreamManagerTest {
	@Mock
	private AppFactory factory;
	@Mock
	private Map<Principal, EventStream> eventStreams;
	@Mock
	private EventStream eventStream;
	@Mock
	EventStreamConfig config;
	@Mock
	private Principal user;
	@Mock
	private Event event;

	private EventStreamManager manager;

	@Before
	public void beforeTest() {
		manager = new EventStreamManagerImpl(factory, eventStreams);
	}

	/** Assert test constructor is marked with {@link TestConstructor} annotation. */
	@Test
	public void testConstructor() throws NoSuchMethodException, SecurityException {
		Constructor<EventStreamManagerImpl> testConstructor = EventStreamManagerImpl.class.getConstructor(AppFactory.class, Map.class);
		assertNotNull("Missing annotation from test constructor.", testConstructor.getAnnotation(TestConstructor.class));
	}

	@Test
	public void push() {
		when(eventStreams.values()).thenReturn(Arrays.asList(eventStream));
		manager.push(event);
		verify(eventStream, times(1)).push(event);
	}

	@Test
	public void push_Principal() {
		when(eventStreams.get(user)).thenReturn(eventStream);
		manager.push(user, event);
		verify(eventStream, times(1)).push(event);
	}

	@Test
	public void push_Collection() {
		List<Principal> users = Arrays.asList(user);
		when(eventStreams.get(user)).thenReturn(eventStream);
		manager.push(users, event);
		verify(eventStream, times(1)).push(event);
	}
}
