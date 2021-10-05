package js.tiny.container.spi;

/**
 * Service metadata is a generic representation for both annotation's metadata and XML deployment descriptor. Container core see
 * all services metadata solely through this interface; it is not aware of concrete meta attributes that are hidden by service
 * implementation.
 * 
 * On bootstrap, container delegates the services for metadata scanning. Service inspects application class annotations and / or
 * descriptor files and create concrete instances for this interface. The container just store them and passes them back on
 * service execution. This way, details about metadata implementation is know only by container service and application logic
 * but not by the container core.
 * 
 * @author Iulian Rotaru
 */
public interface IContainerServiceMeta {

}
