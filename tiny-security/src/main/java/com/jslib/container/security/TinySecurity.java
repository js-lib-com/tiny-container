package com.jslib.container.security;

import java.io.IOException;
import java.security.Principal;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.container.servlet.RequestContext;
import com.jslib.container.servlet.TinyContainer;
import com.jslib.container.spi.IContainer;
import com.jslib.container.spi.ISecurityContext;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.SecurityContext;
import com.jslib.lang.BugError;
import com.jslib.lang.RolesPrincipal;
import com.jslib.util.Params;

class TinySecurity implements ISecurityContext {
	private static final Log log = LogFactory.getLog(TinySecurity.class);

	private final IContainer container;

	public TinySecurity(IContainer container) {
		this.container = container;
	}

	@Override
	public boolean login(String username, String password) {
		log.trace("login(String, String)");
		Params.notNullOrEmpty(username, "User name");
		Params.notNullOrEmpty(password, "Password");

		RequestContext context = container.getInstance(RequestContext.class);

		// container login occurs in two steps:
		// 1. verify credentials - throw exception if credentials are rejected: request.login
		// 2. open authenticated session on cookie: request.authenticate

		final HttpServletRequest request = container.getInstance(HttpServletRequest.class);
		final HttpServletResponse response = context.getResponse();
		if (response == null) {
			throw new BugError("Attempt to use not initialized HTTP response.");
		}

		try {
			request.login(username, password);
			if (!request.authenticate(response)) {
				log.warn("Session authentication fail for user |{user_name}|.", username);
			}
		} catch (ServletException | IOException e) {
			// exception is thrown if request is already authenticated, servlet container authentication is not enabled or
			// credentials are not accepted; consider all these conditions as login fail but record on logger
			log.warn("Login fail for user |{user_name}|. Cause: {exception_message}", username, e.getMessage());
			return false;
		}

		log.info("Login user |{user_name}|.", username);
		return true;
	}

	@Override
	public void authenticate(Principal principal) {
		log.trace("login(Principal)");
		Params.notNull(principal, "User principal");

		HttpSession session = container.getInstance(HttpServletRequest.class).getSession(true);
		if (principal instanceof NonceUser) {
			final NonceUser nonce = (NonceUser) principal;
			session.setMaxInactiveInterval(nonce.getMaxInactiveInterval());
		}

		try {
			session.setAttribute(TinyContainer.ATTR_PRINCIPAL, principal);
		} catch (IllegalStateException e) {
			// improbable condition: exception due to invalid session that was just created
			// it may occur only if another thread temper with login and somehow invalidates the session
			// while is arguable hard to believe, it can theoretically happen an need to be handled
			// anyway, is not a security breach; if storing principal on session fails, session is not authenticated
			log.warn(e);
		}
		log.info("Login principal |{user_principal}|.", principal);
	}

	@Override
	public void logout() {
		log.trace("logout()");
		final HttpServletRequest request = container.getInstance(HttpServletRequest.class);

		Principal principal = getUserPrincipal();
		// user name is only for logging
		String username = principal != null ? principal.getName() : "guest";

		try {
			request.logout();
		} catch (ServletException e) {
			// api-doc is not very explicit about this exception: ' If the logout fails'
			// swallow this exception but record to application logger
			log.warn("Logout fail for user |{user_name}|. Cause: {exception_message}", username, e.getMessage());
		}

		HttpSession session = request.getSession(false);
		if (session != null) {
			// session invalidate takes care to 'unbind any objects bound to it'
			// but just to be on the safe side remove principal attribute explicitly

			try {
				session.removeAttribute(TinyContainer.ATTR_PRINCIPAL);
				session.invalidate();
			} catch (IllegalStateException e) {
				// when enter 'if' block session is valid but could be changed from separated thread
				// swallow this exception but record to application logger
				log.warn(e);
			}
		}
		log.info("Logout user |{user_name}|.", username);
	}

	@Override
	public Principal getUserPrincipal() {
		final HttpServletRequest request = container.getInstance(HttpServletRequest.class);

		// if authentication is provided by servlet container it should be a principal on HTTP request
		// otherwise it must be a session and on session it must be the principal object
		// if none from above just return null

		Principal principal = request.getUserPrincipal();
		if (principal != null) {
			return principal;
		}

		HttpSession session = request.getSession();
		if (session == null) {
			return null;
		}

		try {
			return (Principal) session.getAttribute(TinyContainer.ATTR_PRINCIPAL);
		} catch (IllegalStateException e) {
			// it can happen session to become invalid from another thread
			// this is a legal condition; do not even log it to debug
			return null;
		}
	}

	@Override
	public boolean isAuthorized(String... roles) {
		final HttpServletRequest request = container.getInstance(HttpServletRequest.class);

		// test container provided authorization only if request is authenticated
		if (request.getUserPrincipal() != null) {
			if (roles.length == 0) {
				return true;
			}
			for (String role : roles) {
				if (request.isUserInRole(role)) {
					return true;
				}
			}
			return false;
		}

		// application provided authorization uses principal kept on session
		HttpSession session = request.getSession();
		if (session == null) {
			return false;
		}
		Object attribute = session.getAttribute(TinyContainer.ATTR_PRINCIPAL);
		if (attribute == null) {
			return false;
		}
		if (!(attribute instanceof RolesPrincipal)) {
			log.debug("Attempt to use authorization without roles principal. Authenticated user class is |{java_type}|.", attribute.getClass());
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

	@Override
	public boolean isAuthenticated() {
		return getUserPrincipal() != null;
	}

	@Override
	public boolean isUserInRole(String role) {
		return isAuthorized(role);
	}

	@Override
	public boolean isSecure() {
		try {
			final HttpServletRequest request = container.getInstance(HttpServletRequest.class);
			return request.isSecure();
		} catch (ContextNotActiveException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}

	@Override
	public String getAuthenticationScheme() {
		HttpServletRequest httpRequest = null;
		try {
			httpRequest = container.getInstance(HttpServletRequest.class);
		} catch (ContextNotActiveException e) {
			throw new IllegalStateException(e.getMessage());
		}
		if (httpRequest == null || httpRequest.getAuthType() == null) {
			return null;
		}

		switch (httpRequest.getAuthType()) {
		case HttpServletRequest.BASIC_AUTH:
			return SecurityContext.BASIC_AUTH;

		case HttpServletRequest.CLIENT_CERT_AUTH:
			return SecurityContext.CLIENT_CERT_AUTH;

		case HttpServletRequest.DIGEST_AUTH:
			return SecurityContext.DIGEST_AUTH;

		case HttpServletRequest.FORM_AUTH:
			return SecurityContext.FORM_AUTH;
		}
		return null;
	}
}
