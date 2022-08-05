package com.jslib.tiny.container.mvc.it;

import com.jslib.tiny.container.mvc.View;
import com.jslib.tiny.container.mvc.annotation.Controller;

import jakarta.annotation.security.PermitAll;

@Controller
@PermitAll
class DefaultController {
	public View index() {
		return new PageView();
	}
}