package com.jslib.tiny.container.contextparam;

import jakarta.ejb.EJBException;

/**
 * Missing mandatory context parameter.
 * 
 * @author Iulian Rotaru
 */
public class NoContextParamException extends EJBException {
	private static final long serialVersionUID = -7447014471811470722L;

	public NoContextParamException(String message, Object... args) {
		super(String.format(message, args));
	}
}
