package js.tiny.container.security;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import js.lang.RolesPrincipal;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.servlet.RequestContext;
import js.tiny.container.servlet.TinyContainer;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IInvocation;
import js.tiny.container.spi.IInvocationProcessorsChain;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IMethodInvocationProcessor;
import js.tiny.container.spi.IAnnotationsScanner;

public class SecurityService implements IMethodInvocationProcessor, IAnnotationsScanner {
	private static final Log log = LogFactory.getLog(SecurityService.class);

	private IContainer container;
	private boolean enabled;

	public SecurityService() {
		log.trace("SecurityService()");
	}

	@Override
	public void create(IContainer container) {
		log.trace("create(IContainer)");
		this.container = container;
	}

	@Override
	public Priority getPriority() {
		return Priority.SECURITY;
	}

	@Override
	public List<Annotation> scanClassAnnotations(IManagedClass<?> managedClass) {
		List<Annotation> annotations = new ArrayList<>();

		RolesAllowed rolesAllowed = managedClass.scanAnnotation(RolesAllowed.class);
		if (rolesAllowed != null) {
			annotations.add(rolesAllowed);
		}

		DenyAll denyAll = managedClass.scanAnnotation(DenyAll.class);
		if (denyAll != null) {
			annotations.add(denyAll);
		}

		PermitAll permitAll = managedClass.scanAnnotation(PermitAll.class);
		if (permitAll != null) {
			annotations.add(permitAll);
		}

		if (!annotations.isEmpty()) {
			enabled = true;
		}
		return annotations;
	}

	@Override
	public List<Annotation> scanMethodAnnotations(IManagedMethod managedMethod) {
		List<Annotation> annotations = new ArrayList<>();

		RolesAllowed rolesAllowed = managedMethod.scanAnnotation(RolesAllowed.class);
		if (rolesAllowed != null) {
			annotations.add(rolesAllowed);
		}

		DenyAll denyAll = managedMethod.scanAnnotation(DenyAll.class);
		if (denyAll != null) {
			annotations.add(denyAll);
		}

		PermitAll permitAll = managedMethod.scanAnnotation(PermitAll.class);
		if (permitAll != null) {
			annotations.add(permitAll);
		}

		if (!annotations.isEmpty()) {
			enabled = true;
		}
		return annotations;
	}

	@Override
	public Object onMethodInvocation(IInvocationProcessorsChain chain, IInvocation invocation) throws Exception {
		final IManagedMethod managedMethod = invocation.method();
		// if there is no metadata related to security, grant unchecked access to everything
		if (!enabled) {
			log.debug("Not enabled security service. Grant unchecked access to |%s|!", managedMethod);
			return chain.invokeNextProcessor(invocation);
		}

		final RequestContext requestContext = container.getOptionalInstance(RequestContext.class);
		// grant unchecked access for methods executed outside HTTP request
		// e.g. post construct executed from main thread at container startup
		if (requestContext == null || !requestContext.isAttached()) {
			log.debug("Attempt use security service outside HTTP request. Grant unchecked access to |%s|!", managedMethod);
			return chain.invokeNextProcessor(invocation);
		}

		if (isDenyAll(managedMethod)) {
			log.warn("Access denied to |%s|.", managedMethod);
			throw new AuthorizationException();
		}

		if (isPermitAll(managedMethod)) {
			log.debug("Public access to |%s|.", managedMethod);
			return chain.invokeNextProcessor(invocation);
		}

		if (!isAuthorized(requestContext.getRequest(), getRoles(managedMethod))) {
			log.info("Reject not authorized access to |%s|.", managedMethod);
			throw new AuthorizationException();
		}

		return chain.invokeNextProcessor(invocation);
	}

	// --------------------------------------------------------------------------------------------

	private static boolean isDenyAll(IManagedMethod managedMethod) {
		return managedMethod.getAnnotation(DenyAll.class) != null || managedMethod.getDeclaringClass().getAnnotation(DenyAll.class) != null;
	}

	private static boolean isPermitAll(IManagedMethod managedMethod) {
		return managedMethod.getAnnotation(PermitAll.class) != null || managedMethod.getDeclaringClass().getAnnotation(PermitAll.class) != null;
	}

	private static final String[] EMPTY_ROLES = new String[0];

	private static String[] getRoles(IManagedMethod managedMethod) {
		RolesAllowed rolesAllowed = managedMethod.getAnnotation(RolesAllowed.class);
		if (rolesAllowed == null) {
			rolesAllowed = managedMethod.getDeclaringClass().getAnnotation(RolesAllowed.class);
		}
		return rolesAllowed != null ? rolesAllowed.value() : EMPTY_ROLES;
	}

	private static boolean isAuthorized(HttpServletRequest httpRequest, String[] roles) {
		// test container provided authorization only if request is authenticated
		if (httpRequest.getUserPrincipal() != null) {
			if (roles.length == 0) {
				return true;
			}
			for (String role : roles) {
				if (httpRequest.isUserInRole(role)) {
					return true;
				}
			}
			return false;
		}

		// application provided authorization uses principal kept on session
		HttpSession session = httpRequest.getSession();
		if (session == null) {
			return false;
		}
		Object attribute = session.getAttribute(TinyContainer.ATTR_PRINCIPAL);
		if (attribute == null) {
			return false;
		}
		if (!(attribute instanceof RolesPrincipal)) {
			log.bug("Attempt to use authorization without roles principal. Authenticated user class is |%s|.", attribute.getClass());
			return false;
		}

		if (roles.length == 0) {
			return true;
		}
		RolesPrincipal principal = (RolesPrincipal) attribute;
		for (String role : roles) {
			for (String rolePrincipal : principal.getRoles()) {
				if (role.equals(rolePrincipal)) {
					return true;
				}
			}
		}

		return false;
	}
}
