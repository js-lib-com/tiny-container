package js.tiny.container.spi;

public interface IContainerServiceProvider {
	IContainerService createService(IContainer container);
}
