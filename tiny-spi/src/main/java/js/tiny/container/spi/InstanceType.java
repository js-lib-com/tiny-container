package js.tiny.container.spi;

/**
 * Managed instance types. Instance types are used by <code>CDI</code> module to select provider strategy, that is, select the
 * right provision provider. Remember that <code>CDI</code> module uses providers to create instances and that there are two
 * kinds: provisioning and scope providers. Provisioning providers always create new instances whereas scope providers have
 * caches and try to reuse an instance for certain life span.
 * 
 * Instance type is configured into managed class descriptor, <code>type</code> attribute, with default value to {@link #POJO}.
 * In sample descriptor, managed request context class has {@link #POJO} scope. It is developer responsibility to use a
 * supported <code>type</code> value otherwise managed class initialization fails with application abort.
 * 
 * <pre>
 * &lt;request-context class="js.servlet.RequestContext" type="POJO" /&gt;
 * </pre>
 * 
 * There are predefined instance type constants.
 * <table summary="Instance Types">
 * <tr>
 * <th>Type
 * <th>Description
 * <tr>
 * <td>POJO
 * <td>Plain Old Java Object. This type of managed objects are standard Java instances; they support only life cycle management
 * and dependency injection but no other services like declarative transaction, provided by {@link #PROXY}. CDI provider simple
 * returns implementation instance and method invocation is resolved directly by virtual machine.
 * <tr>
 * <td>PROXY
 * <td>Managed Proxy. CDI provider replaces implementation class with a dynamically generated Java Proxy which in turn invokes
 * implementation methods using reflection. This, of course, comes with a small overhead; please consider {@link #POJO}
 * alternative if container services are not needed.
 * <tr>
 * <td>REMOTE
 * <td>Managed instance on foreign server. Managed class descriptor should include the URL of the context where remote class is
 * deployed. When create remote types, application factory creates a Java Proxy that relay method invocation via HTTP-RMI to
 * remote implementation.
 * <tr>
 * <td>SERVICE
 * <td>External service loaded by Java service loader. In this context <em>external</em> means outside application scope but in
 * the same virtual machine.
 * </table>
 * 
 * <b>Warning:</b> use a supported <code>type</code> value into managed class descriptor or application fails to start.
 * 
 * @author Iulian Rotaru
 */
public enum InstanceType {
	/**
	 * Plain Old Java Object. This type of managed objects are standard Java instances; they support only life cycle management
	 * and dependency injection but no other services like declarative transaction, provided by {@link #PROXY}. CDI provider
	 * simple returns implementation instance and method invocation is resolved directly by virtual machine.
	 */
	POJO,

	/**
	 * Managed Proxy. CDI provider replaces implementation class with a dynamically generated Java Proxy which in turn
	 * invokes implementation methods using reflection. This, of course, comes with a small overhead; please consider
	 * {@link #POJO} alternative if container services are not needed.
	 */
	PROXY,

	/**
	 * Managed instance on foreign server. Managed class descriptor should include the URL of the context where remote class is
	 * deployed. When create remote types, application factory creates a Java Proxy that relay method invocation via HTTP-RMI to
	 * remote implementation.
	 */
	REMOTE,

	/**
	 * External service loaded by Java service loader. In this context <em>external</em> means outside application scope but in
	 * the same virtual machine.
	 */
	SERVICE;

	/**
	 * Test if this instance type requires interface on managed class descriptor. If this predicate returns true managed class
	 * descriptor should have <code>interface</code> attribute. More, <code>interface</code> value should be strict Java
	 * interface.
	 * 
	 * @return true if this instance type requires interface declaration.
	 */
	public boolean requiresInterface() {
		return this == PROXY || this == REMOTE;
	}

	/**
	 * Test if this instance type requires class implementation. If this predicate returns true managed class descriptor should
	 * have <code>class</code> attribute. Implementation is used reflexively to create managed instances. This is in contrast
	 * with the other types that loads instances using external means.
	 * 
	 * @return true if this instance type requires class implementation.
	 */
	public boolean requiresImplementation() {
		return this == POJO || this == PROXY;
	}

	/**
	 * Test if this instance type is POJO.
	 * 
	 * @return true if this instance type is POJO.
	 */
	public boolean isPOJO() {
		return this == POJO;
	}

	/**
	 * Test if this instance type is PROXY.
	 * 
	 * @return true if this instance type is PROXY.
	 */
	public boolean isPROXY() {
		return this == PROXY;
	}

	/**
	 * Test if this instance type is REMOTE.
	 * 
	 * @return true if this instance type is REMOTE.
	 */
	public boolean isREMOTE() {
		return this == REMOTE;
	}

	/**
	 * Test if this instance type is SERVICE.
	 * 
	 * @return true if this instance type is SERVICE.
	 */
	public boolean isSERVICE() {
		return this == SERVICE;
	}
}
