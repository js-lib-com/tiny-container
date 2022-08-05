package com.jslib.tiny.container.mvc.it;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.jslib.tiny.container.http.form.Form;
import com.jslib.tiny.container.http.form.FormIterator;
import com.jslib.tiny.container.mvc.View;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

@PermitAll
interface IController {
	View index();

	View text();

	View personView(String name, String profession, int age);

	View processForm(Form form);

	View processMultipartForm(FormIterator form);

	View processFormObject(Person person);

	View uploadForm(Form form) throws FileNotFoundException, IOException;

	View uploadMultipartForm(FormIterator form) throws IOException;

	@RolesAllowed("*")
	View privateAction();

	@RolesAllowed("*")
	View privateForm(Form form);

	View actionException();

	View formException(Form form);
}