package com.jslib.container.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.security.Principal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.api.injector.IBindingBuilder;
import com.jslib.container.spi.AuthorizationException;
import com.jslib.container.spi.IContainer;
import com.jslib.container.spi.IInvocation;
import com.jslib.container.spi.IInvocationProcessorsChain;
import com.jslib.container.spi.IManagedClass;
import com.jslib.container.spi.IManagedMethod;
import com.jslib.container.spi.ISecurityContext;
import com.jslib.container.spi.IMethodInvocationProcessor.Priority;

import jakarta.servlet.http.HttpServletRequest;

@RunWith(MockitoJUnitRunner.class)
public class SecurityServiceTest {
	@Mock
	private IContainer container;
	@Mock
	private IManagedMethod managedMethod;
	@Mock
	private IManagedClass<?> managedClass;

	@Mock
	private IBindingBuilder<ISecurityContext> bindingBuilder;
	@Mock
	private IInvocationProcessorsChain chain;
	@Mock
	private IInvocation invocation;

	@Mock
	private HttpServletRequest httpRequest;

	private SecurityService service;

	@Before
	public void beforeTest() {
		when(container.bind(ISecurityContext.class)).thenReturn(bindingBuilder);
		when(bindingBuilder.instance(any())).thenReturn(bindingBuilder);
		
		when(container.getInstance(HttpServletRequest.class)).thenReturn(httpRequest);
		when(container.getOptionalInstance(HttpServletRequest.class)).thenReturn(httpRequest);
		doReturn(managedClass).when(managedMethod).getDeclaringClass();

		when(invocation.method()).thenReturn(managedMethod);

		service = new SecurityService();
		service.configure(container);
	}

	@Test
	public void GivenDefaults_WhenConfigure_ThenBindingBuild() {
		// given

		// when

		// then
		verify(container, times(1)).bind(any());
		verify(bindingBuilder, times(1)).instance(any());
		verify(bindingBuilder, times(1)).build();
	}

	@Test
	public void GivenDefaults_WhenGetPriority_ThenSECURITY() {
		// given

		// when
		Priority priority = service.getPriority();

		// then
		assertThat(priority, equalTo(Priority.SECURITY));
	}

	@Test
	public void Given_WhenBind_Then() {
		// given

		// when
		service.bind(managedMethod);

		// then
	}

	@Test
	public void GivenMethodJakartaPermitAll_WhenBind_ThenFalse() {
		// given
		Class<? extends Annotation> annotation = jakarta.annotation.security.PermitAll.class;
		doReturn(mock(annotation)).when(managedMethod).scanAnnotation(annotation);

		// when
		boolean bind = service.bind(managedMethod);

		// then
		assertThat(bind, equalTo(false));
	}

	@Test
	public void GivenMethodJakartaDenyAll_WhenBind_ThenTrue() {
		// given
		Class<? extends Annotation> annotation = jakarta.annotation.security.DenyAll.class;
		doReturn(mock(annotation)).when(managedMethod).scanAnnotation(annotation);

		// when
		boolean bind = service.bind(managedMethod);

		// then
		assertThat(bind, equalTo(true));
	}

	@Test
	public void GivenMethodJakartaRolesAllowed_WhenBind_ThenTrue() {
		// given
		Class<? extends Annotation> annotation = jakarta.annotation.security.RolesAllowed.class;
		doReturn(mock(annotation)).when(managedMethod).scanAnnotation(annotation);

		// when
		boolean bind = service.bind(managedMethod);

		// then
		assertThat(bind, equalTo(true));
	}

	@Test
	public void GivenClassJakartaPermitAll_WhenBind_ThenFalse() {
		// given
		Class<? extends Annotation> annotation = jakarta.annotation.security.PermitAll.class;
		doReturn(mock(annotation)).when(managedClass).scanAnnotation(annotation);

		// when
		boolean bind = service.bind(managedMethod);

		// then
		assertThat(bind, equalTo(false));
	}

	@Test
	public void GivenPrincipalRoleMatchDeclaredRole_WhenOnMethodInvocation_ThenChainNext() throws Exception {
		// given
		jakarta.annotation.security.RolesAllowed annotation = mock(jakarta.annotation.security.RolesAllowed.class);
		when(annotation.value()).thenReturn(new String[] { "admin" });
		doReturn(annotation).when(managedMethod).scanAnnotation(jakarta.annotation.security.RolesAllowed.class, IManagedMethod.Flags.INCLUDE_TYPES);

		when(httpRequest.getUserPrincipal()).thenReturn(mock(Principal.class));
		when(httpRequest.isUserInRole("admin")).thenReturn(true);

		// when
		service.onMethodInvocation(chain, invocation);

		// then
		verify(chain, times(1)).invokeNextProcessor(any());
	}

	@Test(expected = AuthorizationException.class)
	public void GivenNoDeclaredRoles_WhenOnMethodInvocation_ThenException() throws Exception {
		// given

		// when
		service.onMethodInvocation(chain, invocation);

		// then
	}

	@Test(expected = AuthorizationException.class)
	public void GivenMethodJakartaDenyAll_WhenOnMethodInvocation_ThenException() throws Exception {
		// given
		Class<? extends Annotation> annotation = jakarta.annotation.security.DenyAll.class;
		doReturn(mock(annotation)).when(managedMethod).scanAnnotation(annotation, IManagedMethod.Flags.INCLUDE_TYPES);

		// when
		service.onMethodInvocation(chain, invocation);

		// then
	}

	@Test
	public void GivenMissingHttpRequest_WhenOnMethodInvocation_ThenChainNext() throws Exception {
		// given
		when(container.getOptionalInstance(HttpServletRequest.class)).thenReturn(null);

		// when
		service.onMethodInvocation(chain, invocation);

		// then
		verify(container, times(1)).getOptionalInstance(any());
		verify(chain, times(1)).invokeNextProcessor(any());
	}
}
