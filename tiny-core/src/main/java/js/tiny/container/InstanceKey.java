package js.tiny.container;

import js.util.Params;

/**
 * Key used to uniquely identify managed instances.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public final class InstanceKey {
	/** Instance key value. */
	private final String value;

	/**
	 * Create instance key with given value.
	 * 
	 * @param value instance key value.
	 * @throws IllegalArgumentException if key value is null.
	 */
	public InstanceKey(String value) {
		Params.notNull(value, "Key value");
		this.value = value;
	}

	/**
	 * Get instance key value.
	 * 
	 * @return instance key value.
	 * @see #value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Get instance key string representation.
	 * 
	 * @return string representation.
	 */
	@Override
	public String toString() {
		return value;
	}

	/**
	 * Get instance key hash code.
	 * 
	 * @return hash code.
	 */
	@Override
	public int hashCode() {
		return value.hashCode();
	}

	/**
	 * Two instance keys are equal if they are the same value.
	 * 
	 * @param obj object to compare for equality.
	 * @return true if given object equals this instance key.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		InstanceKey other = (InstanceKey) obj;
		return value.equals(other.value);
	}
}