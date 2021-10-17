package js.tiny.container.spi;

import js.converter.Converter;

/**
 * Managed class scope controls managed instance life span. Instance scope is used as selector for {@link ScopeFactory}
 * strategy, that is, it selects the right scope factory to use for managed instance retrieval. Note that {@link #LOCAL} scope
 * means managed instance is always fresh created and that there is no scope factory for it.
 * <p>
 * Instance scope is configured into managed class descriptor, <code>scope</code> attribute, with default value to
 * {@link #APPLICATION}. In sample descriptor, managed request context class has {@link #THREAD} scope; as a consequence
 * {@link ThreadScopeFactory} will be used for managed instance retrieval. It is developer responsibility to use a supported
 * <code>scope</code> value otherwise managed class initialization fails with application abort.
 * 
 * <pre>
 * &lt;request-context class="js.servlet.RequestContext" scope="THREAD" /&gt;
 * </pre>
 * 
 * <p>
 * There are predefined scope constants but user defined instance scopes are supported. Anyway, adding a new scope implies also
 * creating a related {@link ScopeFactory}.
 * <table summary="Predefined Scopes">
 * <tr>
 * <th>Scope
 * <th>Factory
 * <th>Description
 * <tr>
 * <td>APPLICATION
 * <td>{@link ApplicationScopeFactory}
 * <td>Application level singleton; one instance per application. Once created, instances with APPLICATION scope last for entire
 * application life span and are destroyed just before application exit.
 * <tr>
 * <td>THREAD
 * <td>{@link ThreadScopeFactory}
 * <td>Thread local storage. When declaring thread is destroyed local storage is freed and THREAD managed instance is garbage
 * collected.
 * <tr>
 * <td>SESSION
 * <td>SessionScopeFactory
 * <td>Stateful across a single HTTP session. Managed instance is stored on HTTP session attribute; when HTTP session expires,
 * attribute is removed and SESSION managed instance becomes candidate for GC.
 * <tr>
 * <td>LOCAL
 * <td>N/A
 * <td>Local scope creates always a new instance that last till declaring code block exit.
 * </table>
 * 
 * <p>
 * <b>Warning:</b> use a supported <code>scope</code> value into managed class descriptor or application fails to start.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public class InstanceScope implements Converter {
	/**
	 * Application level singleton, that is, one instance per application. Instances with APPLICATION scope are destroyed at
	 * application exit.
	 */
	public static final InstanceScope APPLICATION = new InstanceScope("APPLICATION");

	/**
	 * Thread local storage. When declaring thread is destroyed local storage is freed and THREAD managed instance is garbage
	 * collected.
	 */
	public static final InstanceScope THREAD = new InstanceScope("THREAD");

	/**
	 * Stateful across a single HTTP session. Managed instance is stored on HTTP session attribute; when HTTP session expires,
	 * attribute is removed and SESSION managed instance becomes candidate for GC.
	 */
	public static final InstanceScope SESSION = new InstanceScope("SESSION");

	/** Local scope creates always a new instance that last till declaring code block exit. */
	public static final InstanceScope LOCAL = new InstanceScope("LOCAL");

	// --------------------------------------------------------------------------------------------
	// INSTANCE

	/** Scope value. */
	private final String value;

	/** Default constructor. */
	public InstanceScope() {
		this.value = null;
	}

	/**
	 * Construct an immutable scope.
	 * 
	 * @param value scope value.
	 */
	public InstanceScope(String value) {
		this.value = value;
	}

	/**
	 * Get scope value.
	 * 
	 * @return scope value.
	 */
	public String getValue() {
		return value;
	}

	public boolean isLOCAL() {
		return "LOCAL".equals(value);
	}

	/** Scope instance string representation. */
	@Override
	public String toString() {
		return value;
	}

	/** Hash code based on scope value. */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	/** Two scopes are equal if have the same value. */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InstanceScope other = (InstanceScope) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// CONVERTER

	/** Create a scope instance from its string value. */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T asObject(String string, Class<T> valueType) {
		return (T) new InstanceScope(string);
	}

	/** Return string value of given scope object. */
	@Override
	public String asString(Object object) {
		return ((InstanceScope) object).getValue();
	}
}
