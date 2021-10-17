package js.tiny.container.spi;

/**
 * Container service provider is a factory for container services instances. Provider implementation may decide to reuse a
 * singleton instance or to create a new container service when {@link #getService(IContainer)} is invoked.
 * 
 * @author Iulian Rotaru
 */
public interface IContainerServiceProvider {
	
	/**
	 * Get container service instance for requested container. Implementation is free to create new container service instance
	 * every time this method is invoked or to reuse previously created one.
	 * 
	 * @param container parent container.
	 * @return container service instance.
	 */
	IContainerService getService(IContainer container);
	
}
