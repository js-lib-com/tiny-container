package com.jslib.container.mvc.it;

import com.jslib.container.mvc.View;
import com.jslib.container.mvc.annotation.Controller;

import jakarta.annotation.security.PermitAll;

@Controller
@PermitAll
class DefaultController {
	public View index() {
		return new PageView();
	}
}