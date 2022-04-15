package js.tiny.container.security;

import java.lang.annotation.Annotation;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IInvocation;
import js.tiny.container.spi.IInvocationProcessorsChain;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IMethodInvocationProcessor;
import js.tiny.container.spi.ISecurityContext;
import js.util.Classes;

public class SecurityService implements IMethodInvocationProcessor {
	private static final Log log = LogFactory.getLog(SecurityService.class);

	private IContainer container;
	private ISecurityContext security;

	public SecurityService() {
		log.trace("SecurityService()");
	}

	@Override
	public void configure(IContainer container) {
		log.trace("configure(IContainer)");
		this.container = container;
		security = new TinySecurity(container);
		container.bind(ISecurityContext.class).instance(security).build();
	}

	@Override
	public Priority getPriority() {
		return Priority.SECURITY;
	}

	@Override
	public boolean bind(IManagedMethod managedMethod) {
		// if method is 'PermitAll' there is no need to bind security processor
		if (managedMethod.scanAnnotation(PermitAll.class) != null) {
			return false;
		}

		if (managedMethod.scanAnnotation(DenyAll.class) != null) {
			return true;
		}
		if (managedMethod.scanAnnotation(RolesAllowed.class) != null) {
			return true;
		}

		// if declaring class is 'PermitAll' there is no need to bind security processor
		if (managedMethod.getDeclaringClass().scanAnnotation(PermitAll.class) != null) {
			return false;
		}

		// if public access is not granted return true to bind security processor
		return true;
	}

	@Override
	public Object onMethodInvocation(IInvocationProcessorsChain chain, IInvocation invocation) throws Exception {
		final IManagedMethod managedMethod = invocation.method();

		final HttpServletRequest httpRequest = container.getOptionalInstance(HttpServletRequest.class);
		// grant unchecked access for methods executed outside HTTP request
		// e.g. post construct executed from main thread at container startup
		if (httpRequest == null) {
			log.debug("Attempt use security service outside HTTP request. Grant unchecked access to |%s|!", managedMethod);
			return chain.invokeNextProcessor(invocation);
		}

		if (managedMethod.scanAnnotation(DenyAll.class, IManagedMethod.Flags.INCLUDE_TYPES) != null) {
			log.warn("Access denied to |%s|.", managedMethod);
			throw new AuthorizationException();
		}

		if (!security.isAuthorized(getRoles(managedMethod))) {
			log.info("Reject not authorized access to |%s|.", managedMethod);
			throw new AuthorizationException();
		}

		return chain.invokeNextProcessor(invocation);
	}

	// --------------------------------------------------------------------------------------------

	private static final String[] EMPTY_ROLES = new String[0];

	private static String[] getRoles(IManagedMethod managedMethod) {
		Annotation rolesAllowed = managedMethod.scanAnnotation(RolesAllowed.class, IManagedMethod.Flags.INCLUDE_TYPES);
		if (rolesAllowed == null) {
			return EMPTY_ROLES;
		}
		try {
			return Classes.invoke(rolesAllowed, "value");
		} catch (Exception e) {
			log.error(e);
			return EMPTY_ROLES;
		}
	}
}
