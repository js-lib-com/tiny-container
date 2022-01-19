package js.tiny.container.http.encoder;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import js.tiny.container.http.ContentType;

/**
 * Arguments reader key on server encoders registry. When a net method should be invoked, server logic needs to read arguments
 * encoded into HTTP request. There are many protocols carrying arguments depending on servlet implementation and
 * {@link ServerEncoders} registry keeps a map for arguments readers. This class is the key of that map.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public final class EncoderKey {
	/** Constant value for encoder key based on JSON content type. */
	public static final EncoderKey APPLICATION_JSON = new EncoderKey(ContentType.APPLICATION_JSON);

	/** Encoder key value. */
	private final String value;

	/**
	 * Create encoder key based on content type.
	 * 
	 * @param contentType content type.
	 */
	public EncoderKey(ContentType contentType) {
		this(contentType, null);
	}

	/**
	 * Create encoder key for given Java type.
	 * 
	 * @param type Java type.
	 */
	public EncoderKey(Type type) {
		this(null, type);
	}

	/**
	 * Create encoder key for requested content and Java types.
	 * 
	 * @param contentType content type,
	 * @param javaType Java type.
	 */
	public EncoderKey(ContentType contentType, Type javaType) {
		StringBuilder builder = new StringBuilder();
		if (contentType != null) {
			builder.append(contentType.getType());
			builder.append('/');
			builder.append(contentType.getSubtype());
		}
		if (javaType != null) {
			if (builder.length() > 0) {
				builder.append(':');
			}
			if (javaType instanceof ParameterizedType) {
				ParameterizedType parameterizedType = (ParameterizedType) javaType;
				builder.append(((Class<?>) parameterizedType.getRawType()).getName());
			} else {
				builder.append(((Class<?>) javaType).getName());
			}
		}
		this.value = builder.toString();
	}

	// --------------------------------------------------------------------------------------------
	// OBJECT

	/**
	 * Get encoder key string representation.
	 * 
	 * @return string representation.
	 */
	@Override
	public String toString() {
		return value;
	}

	/**
	 * Get hash code for this encoder key instance. This class hash code is based on {@link #value} field.
	 * 
	 * @return hash code.
	 */
	@Override
	public int hashCode() {
		return value.hashCode();
	}

	/**
	 * Predicates that test if given object equals this encoder key instance. Two keys are considered equal if they have the
	 * same {@link #value}.
	 * 
	 * @param obj another object to test for equality, possible null.
	 * @return true if given object is a key with the same value as this instance.
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
		EncoderKey other = (EncoderKey) obj;
		return value.equals(other.value);
	}
}
