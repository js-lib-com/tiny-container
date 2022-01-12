package js.tiny.container.spi;

import jakarta.ejb.EJBException;

public class ServiceConfigurationException extends EJBException {
	private static final long serialVersionUID = 3208319255630034338L;

	public ServiceConfigurationException(String message, Object... args) {
		super(String.format(message, args));
	}

	public ServiceConfigurationException() {
		super();
	}

	public ServiceConfigurationException(Exception ex) {
		super(ex);
	}

	public ServiceConfigurationException(String message, Exception ex) {
		super(message, ex);
	}

	public ServiceConfigurationException(String message) {
		super(message);
	}
}
