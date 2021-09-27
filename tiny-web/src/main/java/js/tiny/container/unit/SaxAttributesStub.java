package js.tiny.container.unit;

import org.xml.sax.Attributes;

public class SaxAttributesStub implements Attributes {
	@Override
	public int getLength() {
		throw new UnsupportedOperationException("getLength()");
	}

	@Override
	public String getURI(int index) {
		throw new UnsupportedOperationException("getURI(int index)");
	}

	@Override
	public String getLocalName(int index) {
		throw new UnsupportedOperationException("getLocalName(int index)");
	}

	@Override
	public String getQName(int index) {
		throw new UnsupportedOperationException("getQName(int index)");
	}

	@Override
	public String getType(int index) {
		throw new UnsupportedOperationException("getType(int index)");
	}

	@Override
	public String getValue(int index) {
		throw new UnsupportedOperationException("getValue(int index)");
	}

	@Override
	public int getIndex(String uri, String localName) {
		throw new UnsupportedOperationException("getIndex(String uri, String localName)");
	}

	@Override
	public int getIndex(String qName) {
		throw new UnsupportedOperationException("getIndex(String qName)");
	}

	@Override
	public String getType(String uri, String localName) {
		throw new UnsupportedOperationException("getType(String uri, String localName)");
	}

	@Override
	public String getType(String qName) {
		throw new UnsupportedOperationException("getType(String qName)");
	}

	@Override
	public String getValue(String uri, String localName) {
		throw new UnsupportedOperationException("getValue(String uri, String localName)");
	}

	@Override
	public String getValue(String qName) {
		throw new UnsupportedOperationException("getValue(String qName)");
	}
}
