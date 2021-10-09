package js.tiny.container.spi;

public abstract class ServiceMeta implements IServiceMeta {
	private final IContainerService service;

	protected ServiceMeta(IContainerService service) {
		this.service = service;
	}

	@Override
	public IContainerService getContainerService() {
		return service;
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return getClass().equals(obj.getClass());
	}
}
