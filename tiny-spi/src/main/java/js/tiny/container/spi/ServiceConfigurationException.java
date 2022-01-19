package js.tiny.container.spi;

import jakarta.ejb.EJBException;

/**
 * Runtime exception thrown if a container service configuration fails, usually because specifications constrains not respected.
 * 
 * @author Iulian Rotaru
 */
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
