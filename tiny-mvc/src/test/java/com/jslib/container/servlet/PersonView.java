package com.jslib.container.servlet;

import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import com.jslib.container.mvc.AbstractView;
import com.jslib.io.VariablesWriter;
import com.jslib.util.Files;

class PersonView extends AbstractView {
	private final Person person;

	public PersonView(Person person) {
		this.person = person;
	}

	@Override
	public void serialize(java.io.OutputStream stream) throws IOException {
		Map<String, String> variables = new HashMap<>();
		variables.put("name", person.name);
		variables.put("profession", person.profession);
		variables.put("age", Integer.toString(person.age));

		VariablesWriter writer = new VariablesWriter(new OutputStreamWriter(stream), variables);
		try {
			Files.copy(new FileReader("src/test/resources/person.html"), writer);
		} catch (IOException e) {
		}
	}
}