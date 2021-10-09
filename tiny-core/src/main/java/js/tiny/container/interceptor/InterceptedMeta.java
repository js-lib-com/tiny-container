package js.tiny.container.interceptor;

import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.ServiceMeta;

class InterceptedMeta extends ServiceMeta {
	private final Class<? extends Interceptor> value;

	public InterceptedMeta(IContainerService service, Intercepted intercepted) {
		super(service);
		this.value = intercepted.value();
	}

	public Class<? extends Interceptor> value() {
		return value;
	}
}
