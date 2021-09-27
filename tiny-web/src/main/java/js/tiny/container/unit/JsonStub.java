package js.tiny.container.unit;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

import js.json.Json;
import js.json.JsonException;

public class JsonStub implements Json {
	@Override
	public <T> T parse(Reader reader, Type type) throws IOException, JsonException, ClassCastException {
		throw new UnsupportedOperationException("parse(Reader reader, Type type)");
	}

	@Override
	public Object[] parse(Reader reader, Type[] types) throws IOException, JsonException {
		throw new UnsupportedOperationException("parse(Reader reader, Type[] types)");
	}

	@Override
	public <T> T parse(String value, Type type) throws JsonException {
		throw new UnsupportedOperationException("parse(String value, Type type)");
	}

	@Override
	public String stringify(Object value) {
		throw new UnsupportedOperationException("stringify(Object value)");
	}

	@Override
	public void stringify(Writer writer, Object value) throws IOException {
		throw new UnsupportedOperationException("stringify(Writer writer, Object value)");
	}
}
