package js.tiny.container.security;

import java.lang.annotation.Annotation;

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
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IMethodInvocationProcessor;
import js.tiny.container.spi.ISecurityContext;
import js.util.Classes;

public class SecurityService implements IMethodInvocationProcessor {
	private static final Log log = LogFactory.getLog(SecurityService.class);

	private IContainer container;

	public SecurityService() {
		log.trace("SecurityService()");
	}

	@Override
	public void configure(IContainer container) {
		container.bind(ISecurityContext.class).to(TinySecurity.class).build();
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
	public boolean bind(IManagedMethod managedMethod) {
		// if method is 'PermitAll' there is no need to bind security processor
		if (managedMethod.scanAnnotation(jakarta.annotation.security.PermitAll.class) != null) {
			return false;
		}
		if (managedMethod.scanAnnotation(javax.annotation.security.PermitAll.class) != null) {
			return false;
		}

		if (managedMethod.scanAnnotation(jakarta.annotation.security.DenyAll.class) != null) {
			return true;
		}
		if (managedMethod.scanAnnotation(javax.annotation.security.DenyAll.class) != null) {
			return true;
		}
		if (managedMethod.scanAnnotation(jakarta.annotation.security.RolesAllowed.class) != null) {
			return true;
		}
		if (managedMethod.scanAnnotation(javax.annotation.security.RolesAllowed.class) != null) {
			return true;
		}

		// if declaring class is 'PermitAll' there is no need to bind security processor
		if (managedMethod.getDeclaringClass().scanAnnotation(jakarta.annotation.security.PermitAll.class) != null) {
			return false;
		}
		if (managedMethod.getDeclaringClass().scanAnnotation(javax.annotation.security.PermitAll.class) != null) {
			return false;
		}

		// if public access is not granted return true to bind security processor
		return true;
	}

	@Override
	public Object onMethodInvocation(IInvocationProcessorsChain chain, IInvocation invocation) throws Exception {
		final IManagedMethod managedMethod = invocation.method();

		final RequestContext requestContext = container.getOptionalInstance(RequestContext.class);
		// grant unchecked access for methods executed outside HTTP request
		// e.g. post construct executed from main thread at container startup
		if (requestContext == null) {
			log.debug("Attempt use security service outside HTTP request. Grant unchecked access to |%s|!", managedMethod);
			return chain.invokeNextProcessor(invocation);
		}

		if (isDenyAll(managedMethod)) {
			log.warn("Access denied to |%s|.", managedMethod);
			throw new AuthorizationException();
		}

		if (!isAuthorized(requestContext.getRequest(), getRoles(managedMethod))) {
			log.info("Reject not authorized access to |%s|.", managedMethod);
			throw new AuthorizationException();
		}

		return chain.invokeNextProcessor(invocation);
	}

	// --------------------------------------------------------------------------------------------

	private static boolean isDenyAll(IManagedMethod managedMethod) {
		if (managedMethod.scanAnnotation(jakarta.annotation.security.DenyAll.class, IManagedMethod.Flags.INCLUDE_TYPES) != null) {
			return true;
		}
		if (managedMethod.scanAnnotation(javax.annotation.security.DenyAll.class, IManagedMethod.Flags.INCLUDE_TYPES) != null) {
			return true;
		}
		return false;
	}

	private static final String[] EMPTY_ROLES = new String[0];

	private static String[] getRoles(IManagedMethod managedMethod) {
		Annotation rolesAllowed = managedMethod.scanAnnotation(jakarta.annotation.security.RolesAllowed.class, IManagedMethod.Flags.INCLUDE_TYPES);
		if (rolesAllowed == null) {
			rolesAllowed = managedMethod.scanAnnotation(javax.annotation.security.RolesAllowed.class, IManagedMethod.Flags.INCLUDE_TYPES);
		}
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
