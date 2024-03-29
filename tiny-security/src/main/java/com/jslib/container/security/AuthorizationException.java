package com.jslib.container.security;

import java.security.GeneralSecurityException;

/**
 * Attempt to access private resource without proper authorization. Thrown by authorization logic when there is an attempt to
 * access a private resource but requester is not authenticated or does not have authorization for that particular resource.
 * <p>
 * In the most generic use case one can access private resources only after authentication. It is reasonable to consider normal
 * flow when access private resources after authentication and breaking this normal flow as exception.
 * 
 * @author Iulian Rotaru
 */
public class AuthorizationException extends GeneralSecurityException {
	private static final long serialVersionUID = 13381077426911761L;

	public AuthorizationException() {
		super();
	}
}
