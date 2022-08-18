package com.jslib.container.servlet;

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