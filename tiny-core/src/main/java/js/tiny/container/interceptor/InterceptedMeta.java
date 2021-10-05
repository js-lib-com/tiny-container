package js.tiny.container.interceptor;

import js.tiny.container.spi.IContainerServiceMeta;

class InterceptedMeta implements IContainerServiceMeta {
	private final Class<? extends Interceptor> value;

	public InterceptedMeta(Intercepted intercepted) {
		this.value = intercepted.value();
	}

	public Class<? extends Interceptor> value() {
		return value;
	}
}
