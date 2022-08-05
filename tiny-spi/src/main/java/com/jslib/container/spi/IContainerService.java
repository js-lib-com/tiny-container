package com.jslib.container.spi;

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

	// TODO: improve container services lifecycle description

	/**
	 * Configure container service. Since this hook is executed before injector start, given container cane be used only to
	 * declare injector bindings but not to retrieve instances.
	 * 
	 * @param container parent container.
	 */
	default void configure(IContainer container) {
	}

	/**
	 * Create and initialize internal state and acquire resources. At this stage injector is created and given container can be
	 * used to retrieve instances declared on service configuration.
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
