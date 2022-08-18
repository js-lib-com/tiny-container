package com.jslib.container.servlet;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;

import com.jslib.container.http.form.Form;
import com.jslib.container.http.form.FormField;
import com.jslib.container.http.form.FormIterator;
import com.jslib.container.http.form.Part;
import com.jslib.container.http.form.UploadStream;
import com.jslib.container.http.form.UploadedFile;
import com.jslib.container.mvc.View;
import com.jslib.container.mvc.annotation.Controller;
import com.jslib.container.mvc.annotation.ResponseContentType;
import com.jslib.container.spi.IContainer;
import com.jslib.util.Files;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;

@Controller("resource")
@PermitAll
class ControllerImpl implements IController {
	private final IContainer container;

	@Inject
	public ControllerImpl(IContainer container) {
		this.container = container;
	}

	@Override
	public View index() {
		return new PageView();
	}

	@ResponseContentType("text/plain;charset=UTF-8")
	@Override
	public View text() {
		return new PageView();
	}

	@Override
	public View personView(String name, String profession, int age) {
		Person p = new Person();
		p.name = name;
		p.profession = profession;
		p.age = age;

		return new PersonView(p);
	}

	@Override
	public View processForm(Form form) {
		Person p = new Person();
		p.name = form.getValue("name");
		p.profession = form.getValue("profession");
		p.age = form.getValue("age", Integer.class);

		return new PersonView(p);
	}

	@Override
	public View processMultipartForm(FormIterator form) {
		Person p = new Person();
		for (Part item : form) {
			if (item instanceof FormField) {
				FormField f = (FormField) item;
				if (f.getName().equals("name")) {
					p.name = f.getValue();
				} else if (f.getName().equals("profession")) {
					p.profession = f.getValue();
				} else if (f.getName().equals("age")) {
					p.age = f.getValue(Integer.class);
				}
			}
		}

		return new PersonView(p);
	}

	@Override
	public View processFormObject(Person person) {
		return new PersonView(person);
	}

	@Override
	public View uploadForm(Form form) throws FileNotFoundException, IOException {
		View view = new PageView();
		UploadedFile upload = form.getUploadedFile("file");
		if (!upload.getContentType().equals("text/plain"))
			return view;
		if (!upload.getFileName().equals("test-file.txt"))
			return view;
		
		StringWriter writer = new StringWriter();
		Files.copy(new FileReader(upload.getFile()), writer);
		
		App app = container.getInstance(App.class);
		app.content = writer.toString();
		return view;
	}

	@Override
	public View uploadMultipartForm(FormIterator form) throws IOException {
		View view = new PageView();
		for (Part item : form) {
			if (item instanceof UploadStream) {
				UploadStream upload = (UploadStream) item;
				if (!upload.getContentType().equals("text/plain")) {
					return view;
				}
				if (!upload.getFileName().equals("test-file.txt")) {
					return view;
				}
				StringWriter writer = new StringWriter();
				Files.copy(new InputStreamReader(upload.openStream(), "UTF-8"), writer);
				
				App app = container.getInstance(App.class);
				app.content = writer.toString();
			}
		}
		return view;
	}

	@Override
	public View privateAction() {
		return new PageView();
	}

	@Override
	public View privateForm(Form form) {
		return new PageView();
	}

	@Override
	public View actionException() {
		throw new RuntimeException("Action exception.");
	}

	@Override
	public View formException(Form form) {
		throw new RuntimeException("Form exception.");
	}
}