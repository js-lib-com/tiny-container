package js.tiny.container.cdi;

/**
 * Signal new instance created. This listener is signaled only when
 * {@link CDI#getInstance(Class, IInstancePostConstructionListener)} creates a new instance but not when instance is retrieved
 * from scope cache.
 * 
 * @author Iulian Rotaru
 */
public interface IInstancePostConstructionListener<T> {

	void onInstancePostConstruction(T instance);

}
