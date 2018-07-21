package js.unit;

import javax.servlet.http.Cookie;

public class CookieStub extends Cookie {
	private static final long serialVersionUID = 7107160604081441612L;

	public CookieStub(String name, String value) {
		super(name, value);
	}

	@Override
	public Object clone() {
		throw new UnsupportedOperationException("clone()");
	}

	@Override
	public String getComment() {
		throw new UnsupportedOperationException("getComment()");
	}

	@Override
	public String getDomain() {
		throw new UnsupportedOperationException("getDomain()");
	}

	@Override
	public int getMaxAge() {
		throw new UnsupportedOperationException("getMaxAge()");
	}

	@Override
	public String getName() {
		throw new UnsupportedOperationException("getName()");
	}

	@Override
	public String getPath() {
		throw new UnsupportedOperationException("getPath()");
	}

	@Override
	public boolean getSecure() {
		throw new UnsupportedOperationException("getSecure()");
	}

	@Override
	public String getValue() {
		throw new UnsupportedOperationException("getValue()");
	}

	@Override
	public int getVersion() {
		throw new UnsupportedOperationException("getVersion()");
	}

	@Override
	public boolean isHttpOnly() {
		throw new UnsupportedOperationException("isHttpOnly()");
	}

	@Override
	public void setComment(String purpose) {
		throw new UnsupportedOperationException("setComment(String purpose)");
	}

	@Override
	public void setDomain(String pattern) {
		throw new UnsupportedOperationException("setDomain(String pattern)");
	}

	@Override
	public void setHttpOnly(boolean httpOnly) {
		throw new UnsupportedOperationException("setHttpOnly(boolean httpOnly)");
	}

	@Override
	public void setMaxAge(int expiry) {
		throw new UnsupportedOperationException("setMaxAge(int expiry)");
	}

	@Override
	public void setPath(String uri) {
		throw new UnsupportedOperationException("setPath(String uri)");
	}

	@Override
	public void setSecure(boolean flag) {
		throw new UnsupportedOperationException("setSecure(boolean flag)");
	}

	@Override
	public void setValue(String newValue) {
		throw new UnsupportedOperationException("setValue(String newValue)");
	}

	@Override
	public void setVersion(int v) {
		throw new UnsupportedOperationException("setVersion(int v)");
	}
}
