package js.tiny.container.mvc;

import js.tiny.container.spi.IServiceMeta;

public class ControllerMeta implements IServiceMeta {
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
