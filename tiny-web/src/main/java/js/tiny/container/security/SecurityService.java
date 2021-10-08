package js.tiny.container.security;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import js.lang.InvocationException;
import js.lang.RolesPrincipal;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.servlet.RequestContext;
import js.tiny.container.servlet.TinyContainer;
import js.tiny.container.spi.AuthorizationException;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IContainerServiceMeta;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IMethodInvocation;
import js.tiny.container.spi.IMethodInvocationProcessor;
import js.tiny.container.spi.IMethodInvocationProcessorsChain;

public class SecurityService implements IContainerService, IMethodInvocationProcessor {
	private static final Log log = LogFactory.getLog(SecurityService.class);

	private final IContainer container;

	public SecurityService(IContainer container) {
		log.trace("SecurityService(IContainer)");
		this.container = container;
	}

	@Override
	public Priority getPriority() {
		return Priority.SECURITY;
	}

	@Override
	public List<IContainerServiceMeta> scan(IManagedClass managedClass) {
		List<IContainerServiceMeta> servicesMeta = new ArrayList<>();

		RolesAllowed rolesAllowed = managedClass.getAnnotation(RolesAllowed.class);
		if (rolesAllowed != null) {
			servicesMeta.add(new RolesAllowedMeta(rolesAllowed));
		}

		DenyAll denyAll = managedClass.getAnnotation(DenyAll.class);
		if (denyAll != null) {
			servicesMeta.add(new DenyAllMeta());
		}

		PermitAll permitAll = managedClass.getAnnotation(PermitAll.class);
		if (permitAll != null) {
			servicesMeta.add(new PermitAllMeta());
		}

		return servicesMeta;
	}

	@Override
	public List<IContainerServiceMeta> scan(IManagedMethod managedMethod) {
		List<IContainerServiceMeta> servicesMeta = new ArrayList<>();

		RolesAllowed rolesAllowed = managedMethod.getAnnotation(RolesAllowed.class);
		if (rolesAllowed != null) {
			servicesMeta.add(new RolesAllowedMeta(rolesAllowed));
		}

		DenyAll denyAll = managedMethod.getAnnotation(DenyAll.class);
		if (denyAll != null) {
			servicesMeta.add(new DenyAllMeta());
		}

		PermitAll permitAll = managedMethod.getAnnotation(PermitAll.class);
		if (permitAll != null) {
			servicesMeta.add(new PermitAllMeta());
		}

		return servicesMeta;
	}

	@Override
	public Object invoke(IMethodInvocationProcessorsChain processorsChain, IMethodInvocation methodInvocation) throws AuthorizationException, IllegalArgumentException, InvocationException {
		final IManagedMethod managedMethod = methodInvocation.method();

		final RequestContext requestContext = container.getInstance(RequestContext.class);
		final HttpServletRequest httpRequest = requestContext.getRequest();
		if (httpRequest == null) {
			log.debug("Attempt use security service outside HTTP request. Grant not authenticated access to |%s|!", managedMethod);
			return processorsChain.invokeNextProcessor(methodInvocation);
		}

		if (isDenyAll(managedMethod)) {
			log.warn("Access denied to |%s|.", managedMethod);
			throw new AuthorizationException();
		}

		if (isPermitAll(managedMethod)) {
			log.debug("Public access to |%s|.", managedMethod);
			return processorsChain.invokeNextProcessor(methodInvocation);
		}

		if (!isAuthorized(httpRequest, getRoles(managedMethod))) {
			log.info("Reject not authorized access to |%s|.", managedMethod);
			throw new AuthorizationException();
		}

		return processorsChain.invokeNextProcessor(methodInvocation);
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	// --------------------------------------------------------------------------------------------

	private static boolean isDenyAll(IManagedMethod managedMethod) {
		return managedMethod.getServiceMeta(DenyAllMeta.class) != null || managedMethod.getDeclaringClass().getServiceMeta(DenyAllMeta.class) != null;
	}

	private static boolean isPermitAll(IManagedMethod managedMethod) {
		return managedMethod.getServiceMeta(PermitAllMeta.class) != null || managedMethod.getDeclaringClass().getServiceMeta(PermitAllMeta.class) != null;
	}

	private static final String[] EMPTY_ROLES = new String[0];

	private static String[] getRoles(IManagedMethod managedMethod) {
		RolesAllowedMeta rolesAllowed = managedMethod.getServiceMeta(RolesAllowedMeta.class);
		if (rolesAllowed == null) {
			rolesAllowed = managedMethod.getDeclaringClass().getServiceMeta(RolesAllowedMeta.class);
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
