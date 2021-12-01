package js.tiny.container.mvc.it;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import js.tiny.container.http.form.Form;
import js.tiny.container.http.form.FormIterator;
import js.tiny.container.mvc.View;

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