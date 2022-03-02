package js.tiny.container.rest;

import jakarta.ejb.EJBException;

public class ContextInjectionException extends EJBException {
	private static final long serialVersionUID = -3158638921603450821L;

	public ContextInjectionException(String message, Object... args) {
		super(String.format(message, args));
	}

	public ContextInjectionException() {
		super();
	}

	public ContextInjectionException(Exception ex) {
		super(ex);
	}

	public ContextInjectionException(String message, Exception ex) {
		super(message, ex);
	}

	public ContextInjectionException(String message) {
		super(message);
	}
}
