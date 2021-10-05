package js.tiny.container.mvc;

import js.tiny.container.mvc.annotation.Controller;
import js.tiny.container.spi.IContainerServiceMeta;

class ControllerMeta implements IContainerServiceMeta {
	private final String value;

	public ControllerMeta(Controller controller) {
		this.value = controller.value();
	}

	public ControllerMeta(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}
}
