package js.tiny.container.cdi;

import java.util.Objects;

import js.util.Strings;

public class Key<T> {

	public static <T> Key<T> get(Class<T> type) {
		return new Key<>(type);
	}

	public static <T> Key<T> get(Class<T> type, Object qualifier) {
		return new Key<>(type, qualifier);
	}

	// --------------------------------------------------------------------------------------------
	
	private final Class<T> type;
	private Object qualifier;

	public Key(Class<T> type) {
		this.type = type;
	}

	public Key(Class<T> type, Object qualifier) {
		this.type = type;
		this.qualifier = qualifier;
	}

	public Class<T> type() {
		return type;
	}
	
	public void setQualifier(Object qualifier) {
		this.qualifier = qualifier;
	}

	@Override
	public int hashCode() {
		return Objects.hash(qualifier, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Key<?> other = (Key<?>) obj;
		return Objects.equals(qualifier, other.qualifier) && Objects.equals(type, other.type);
	}

	@Override
	public String toString() {
		return Strings.toString(type, qualifier);
	}
	
}
