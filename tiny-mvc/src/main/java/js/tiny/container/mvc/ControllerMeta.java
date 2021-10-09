package js.tiny.container.mvc;

import js.tiny.container.mvc.annotation.Controller;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.ServiceMeta;

class ControllerMeta extends ServiceMeta {
	private final String value;

	public ControllerMeta(IContainerService service, Controller controller) {
		super(service);
		this.value = controller.value();
	}

	public ControllerMeta(IContainerService service, String value) {
		super(service);
		this.value = value;
	}

	public String value() {
		return value;
	}
}
