package com.jslib.container.ejb;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;

import com.jslib.lang.NoSuchBeingException;
import com.jslib.loadbalancer.INode;
import com.jslib.loadbalancer.LoadBalancer;

@RunWith(MockitoJUnitRunner.class)
public class EjbProxyHandlerTest {
	@Mock
	private LoadBalancer loadBalancer;
	@Mock
	private INode node;

	private ClientAndServer server;
	private EjbProxyHandler proxy;

	@Before
	public void beforeTest() {
		when(loadBalancer.getNode()).thenReturn(node);
		when(node.getImplementationURL()).thenReturn("http://localhost:1964/");

		server = startClientAndServer(1964);
		proxy = new EjbProxyHandler(loadBalancer);
	}

	@After
	public void afterTest() {
		server.stop();
	}

	@Test
	public void Given200_WhenInvoke_Then() throws Throwable {
		// given
		server.when(//
				request() //
						.withMethod("POST")//
						.withPath("/com/jslib/container/ejb/EjbProxyHandlerTest/IService/getPerson.rmi")//
						.withHeader(new Header("Content-Type", "application/json"))//
						.withBody("[\"Iulian Rotaru\"]")) //
				.respond(response()//
						.withStatusCode(200)//
						.withHeader(new Header("Content-Type", "application/json"))//
						.withBody("{\"name\":\"Iulian Rotaru\"}"));

		// when
		Object value = proxy.invoke(null, IService.class.getMethod("getPerson", new Class<?>[] { String.class }), new Object[] { "Iulian Rotaru" });

		// then
		assertThat(value, notNullValue());
		assertThat(value, instanceOf(Person.class));

		Person person = (Person) value;
		assertThat(person.name, equalTo("Iulian Rotaru"));
	}

	/**
	 * A condition encountered on production in a not understood context: RMI exception for no arguments constructor.
	 */
	@Test(expected = NoSuchBeingException.class)
	public void GivenRmiExceptionWithoutArguments_WhenInvoke_ThenNoSuchBeingException() throws Throwable {
		// given
		server.when(//
				request() //
						.withMethod("POST")//
						.withPath("/com/jslib/container/ejb/EjbProxyHandlerTest/IService/getPerson.rmi")//
						.withHeader(new Header("Content-Type", "application/json"))//
						.withBody("[\"Iulian Rotaru\"]")) //
				.respond(response()//
						.withStatusCode(500)//
						.withHeader(new Header("Content-Type", "application/json"))//
						.withBody("{\"exceptionClass\":\"com.jslib.rmi.RmiException\",\"constructorArguments\":[]}"));

		// when
		proxy.invoke(null, IService.class.getMethod("getPerson", new Class<?>[] { String.class }), new Object[] { "Iulian Rotaru" });

		// then
	}

	private static class Person {
		public String name;
	}

	private static interface IService {
		Person getPerson(String name);
	}
}
