package js.tiny.container.mvc.it;

import javax.annotation.security.PermitAll;

import js.tiny.container.mvc.View;
import js.tiny.container.mvc.annotation.Controller;

@Controller
@PermitAll
class DefaultController {
	public View index() {
		return new PageView();
	}
}