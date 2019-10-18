package js.tiny.container;

import js.converter.Converter;
import js.converter.ConverterException;
import js.util.Params;

/**
 * Managed instance types. Instance type is used as selector for {@link InstanceFactory} strategy, that is, it selects the right
 * instance factory to use for managed instance creation.
 * <p>
 * Instance type is configured into managed class descriptor, <code>type</code> attribute, with default value to {@link #POJO}.
 * In sample descriptor, managed request context class has {@link #POJO} scope; as a consequence {@link LocalInstanceFactory}
 * will be used for managed instance creation. It is developer responsibility to use a supported <code>type</code> value
 * otherwise managed class initialization fails with application abort.
 * 
 * <pre>
 * &lt;request-context class="js.servlet.RequestContext" type="POJO" /&gt;
 * </pre>
 * 
 * <p>
 * There are predefined instance type constants but user defined instance scopes are supported. Anyway, adding a new scope
 * implies also creating a related {@link InstanceFactory}.
 * <table summary="Predefined Types">
 * <tr>
 * <th>Type
 * <th>Factory
 * <th>Description
 * <tr>
 * <td>POJO
 * <td>{@link LocalInstanceFactory}
 * <td>Plain Old Java Object. This type of managed objects are standard java instances; they support only life cycle management
 * and dependency injection but no other services like declarative transaction, provided by {@link #PROXY}. Factory simple
 * returns implementation instance and method invocation is resolved directly by virtual machine.
 * <tr>
 * <td>PROXY
 * <td>{@link LocalInstanceFactory}
 * <td>Managed Proxy. Application factory replaces implementation class with a dynamically generated Java Proxy which in turn
 * invokes implementation methods using reflection. This, of course, comes with a small overhead; please consider {@link #POJO}
 * alternative if container services are not needed.
 * <tr>
 * <td>REMOTE
 * <td>{@link RemoteInstanceFactory}
 * <td>Managed instance on foreign server. Managed class descriptor should include the URL of the context where remote class is
 * deployed. When create remote types, application factory creates a Java Proxy that relay method invocation via HTTP-RMI to
 * remote implementation.
 * <tr>
 * <td>SERVICE
 * <td>{@link ServiceInstanceFactory}
 * <td>External service loaded by Java service loader. In this context <em>external</em> means outside application scope but in
 * the same virtual machine.
 * </table>
 * 
 * <p>
 * <b>Warning:</b> use a supported <code>type</code> value into managed class descriptor or application fails to start.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public class InstanceType implements Converter {
	/**
	 * Plain Old Java Object. This type of managed objects are standard java instances; they support only life cycle management
	 * and dependency injection but no other services like declarative transaction, provided by {@link #PROXY}. Factory simple
	 * returns implementation instance and method invocation is resolved directly by virtual machine.
	 */
	public static final InstanceType POJO = new InstanceType("POJO");

	/**
	 * Managed Proxy. Application factory replaces implementation class with a dynamically generated Java Proxy which in turn
	 * invokes implementation methods using reflection. This, of course, comes with a small overhead; please consider
	 * {@link #POJO} alternative if container services are not needed.
	 */
	public static final InstanceType PROXY = new InstanceType("PROXY");

	/**
	 * Managed instance on foreign server. Managed class descriptor should include the URL of the context where remote class is
	 * deployed. When create remote types, application factory creates a Java Proxy that relay method invocation via HTTP-RMI to
	 * remote implementation.
	 */
	public static final InstanceType REMOTE = new InstanceType("REMOTE");

	/**
	 * External service loaded by Java service loader. In this context <em>external</em> means outside application scope but in
	 * the same virtual machine.
	 */
	public static final InstanceType SERVICE = new InstanceType("SERVICE");

	// --------------------------------------------------------------------------------------------
	// INSTANCE

	/** Instance type value. */
	private final String value;

	/** Default constructor for converter instance. */
	public InstanceType() {
		this.value = null;
	}

	/**
	 * Create immutable instance type.
	 * 
	 * @param value instance type value.
	 * @throws IllegalArgumentException if instance type value is null.
	 */
	public InstanceType(String value) {
		Params.notNull(value, "Instance type value");
		this.value = value;
	}

	/**
	 * Get instance type value.
	 * 
	 * @return instance type value.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Test if this instance type requires interface on managed class descriptor. If this predicate returns true managed class
	 * descriptor should have <code>interface</code> attribute. More, <code>interface</code> value should be strict Java
	 * interface.
	 * 
	 * @return true if this instance type requires interface declaration.
	 */
	public boolean requiresInterface() {
		return PROXY.value.equals(value) || REMOTE.value.equals(value);
	}

	/**
	 * Test if this instance type requires class implementation. If this predicate returns true managed class descriptor should
	 * have <code>class</code> attribute. Implementation is used reflexively to create managed instances. This is in contrast
	 * with the other types that loads instances using external means.
	 * 
	 * @return true if this instance type requires class implementation.
	 */
	public boolean requiresImplementation() {
		return POJO.value.equals(value) || PROXY.value.equals(value);
	}

	/**
	 * Test if this instance type is POJO.
	 * 
	 * @return true if this instance type is POJO.
	 */
	public boolean isPOJO() {
		return POJO.value.equals(value);
	}

	/**
	 * Test if this instance type is PROXY.
	 * 
	 * @return true if this instance type is PROXY.
	 */
	public boolean isPROXY() {
		return PROXY.value.equals(value);
	}

	/**
	 * Test if this instance type is REMOTE.
	 * 
	 * @return true if this instance type is REMOTE.
	 */
	public boolean isREMOTE() {
		return REMOTE.value.equals(value);
	}

	/**
	 * Test if this instance type is SERVICE.
	 * 
	 * @return true if this instance type is SERVICE.
	 */
	public boolean isSERVICE() {
		return SERVICE.value.equals(value);
	}

	/** Instance type string representation. */
	@Override
	public String toString() {
		return value;
	}

	/** Hash code based on instance type value. */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	/** Two instance types are equal if have the same values. */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InstanceType other = (InstanceType) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// CONVERTER

	/** Create an instance type from its string value. */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T asObject(String string, Class<T> valueType) throws IllegalArgumentException, ConverterException {
		return (T) new InstanceType(string);
	}

	/** Return string value of given instance type. */
	@Override
	public String asString(Object object) throws ConverterException {
		return ((InstanceType) object).getValue();
	}
}
