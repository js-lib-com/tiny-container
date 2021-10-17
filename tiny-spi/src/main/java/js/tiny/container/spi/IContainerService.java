package js.tiny.container.spi;

/**
 * A piece of functionality provided by container that can be loaded at runtime, using Java services loader facility. Container
 * service instance is specialized by implementing specific join point interfaces - see {@link IFlowProcessor}.
 * 
 * Container service implementation should not contain any mutable state so that it can be safely reused, and behave civilized
 * in multi-threaded running environments.
 * 
 * @author Iulian Rotaru
 */
public interface IContainerService {

	/**
	 * Create and initialize container service.
	 * 
	 * @param container parent container.
	 */
	default void create(IContainer container) {
	}

	/**
	 * Release resources used by container service.
	 */
	default void destroy() {
	}

}
