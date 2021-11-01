# Managed Class

A managed class is a component of an application designed to be executed into a container. It is focused strictly on business concerns, whereas aditional, cross-cutting concerns are handled by container.

Application developer is not required to be an expert at system-level programming; usually does not program transactions, concurrency, security, distribution, or other services into the application logic but relies on container for these.

The essential characteristics of a managed class are:

- It typically contains only business logic that operates on the business data.
- Instances are managed at runtime by container.
- Can be customized at deployment time by editing its environment entries.
- Various service information, such as transaction and security attributes, may be specified together with the business logic in the form of metadata annotations, or separately, in an XML deployment descriptor.
- Client access is mediated by the container in which the managed class is deployed.
- A managed class can be included in an assembled application without requiring source code changes or recompilation.

---

 Managed class resemble Java {@link Class} and is configurable from external class descriptor and by annotations. Class
 descriptor is a configuration element from <code>managed-classes</code> section from application descriptor. Managed class
 parses and validates class descriptor and annotations and initialize its state accordingly; managed class is immutable. Also
 takes care to parse managed class configuration, if exist into application descriptor, see below <code>Descriptor</code>
 section. Anyway, managed class has no means to create instances, like Java class has. This is because instance creation
 algorithm is implemented into container and exposes by {@link IFactory} interface.
 <p>
 As stated, managed class parses given class descriptor and scans all interfaces and implementation classes annotations in
 order to initialize its internal state. Since is immutable, initialization is fully completed on construction, there is no
 setters. If constructor fails to validate its internal state throws {@link ConfigException} and container creation aborts.
 <p>
 Managed class holds information used by container for managed instances life cycle, that is, instances creation or reusing,
 initialization and instance declarative services. Here are managed class core properties and related class descriptor
 attributes, used by container for instances life cycle:
 <ul>
 <li>{@link #interfaceClasses} used to identify managed instances - initialized from <code>interface</code> attribute or child
 element,
 <li>{@link #implementationClass} used to create instances when managed class is local - initialized from <code>class</code>
 attribute,
 <li>{@link #instanceType} selector for CDI provisioning providers strategy - initialized from <code>type</code> attribute,
 <li>{@link #instanceScope} selector for CDI scoped providers strategy - initialized from <code>scope</code> attribute.
 </ul>
 <p>
 Container uses interface classes to identify managed classes in classes pool. For this reason is not possible to use the same
 interface class for two different managed classes.
 <p>
 Managed class has an interface and associated implementation. Usually there is only one interface but support for multiple
 interface exists. Note that <code>interface</code> concept is not identical with Java interface. It is in fact the class used
 to identify managed class and related instances; <code>interfaceClass</code> parameter from
 {@link IFactory#getInstance(Class, Object...)} refers to this <code>interface</code>. In most cases it is indeed a Java
 interface but can be abstract class or even standard Java class. Implementation class is optional depending on
 {@link #instanceType}. Anyway, if implementation exists it must be an instantiable class, package private accepted.

 <h3>Class Descriptor</h3> Managed classes use class descriptors to declare managed class interface(s), implementation, type
 and scope. In application descriptor there is a predefined <code>managed-classes</code> section and inside it all managed
 classes are declared, an element per class - this configuration element is the class descriptor. Every managed class
 descriptor has a name, i.e. the element tag; this name is used to declare managed class configuration section.
 <p>
 Lets consider a UserManagerImpl class that implements UserManager interface. Into application descriptor there is
 <code>managed-classes</code> section used to declare all managed classes. Below <code>user-manager</code> section is managed
 class specific configuration.

 <pre>
 class UserManagerImpl implements UserManager {
 	static File USERS_PATH;
 }

 // application descriptor
 ...
 // managed classes section
 &lt;managed-classes&gt;
 	// class descriptor for managed class
 	&lt;user-manager interface="js.admin.UserManager" implementation="js.admin.UserManagerImpl" type="POJO" scope="APPLICATION" /&gt;
 	...
 &lt;/managed-classes&gt;

 // managed class configuration
 &lt;user-manager&gt;
 	&lt;static-field name="USERS_PATH" value="/usr/share/tomcat/conf/users" /&gt;
 &lt;/manager&gt;
 </pre>

 In above sample code, descriptor class for js.admin.UserManager has <code>user-manager</code> name. This name is used to
 declare managed class configuration - the below section with the same name. Class configuration section is supplied to
 {@link Configurable#config(Config)}, of course if managed class implements {@link Configurable}. Every managed class
 configuration section has a specific content that has meaning only to owning managed class.
 <p>
 Managed classes support multiple interfaces. In sample, transactional resource is a single POJO instance with application
 scope. It is accessible by both its own class and by transaction context interface. No mater which interface is used to
 retrieve the managed instance, the same managed class is used in the end.

 <pre>
 &lt;data-source class="js.core.TransactionalResource"&gt;
 	&lt;interface name="js.core.TransactionalResource" /&gt;
 	&lt;interface name="js.core.TransactionContext" /&gt;
 &lt;/data-source&gt;
 </pre>
 <p>
 Also, a managed instance can be loaded from a service using Java service loader. In this case managed class has no
 implementation declared and has {@link InstanceType#SERVICE} type.

 <pre>
 &lt;transaction-manager interface="js.core.TransactionManager" type="SERVICE" /&gt;
 </pre>
 <p>
 Finally, a managed class could describe a managed instance deployed on a remote host. For this uses
 {@link InstanceType#REMOTE} type and add <code>url</code> attribute with remote host address. Managed instances created for
 this managed class will actually be a Java proxy that delegates method invocation to a HTTP-RMI client.

 <pre>
 &lt;weather-service interface="ro.bbnet.WeatherService" type="REMOTE" url="http://bbnet.ro" /&gt;
 </pre>

 Even if <code>url</code> attribute is provided, when use {@link IFactory#getInstance(String, Class, Object...)} to retrieve
 named instances, container will enact a discovery process based on provided instance name that in this case should be unique
 on local network.

 <h3 id="annotations">Annotations</h3>
 <p>
 Managed class supports declarative services via annotations. This implementation follows JSR-250 guidelines for annotations
 inheritance. Basically annotations are searched in implementation class; this is true for method annotations which are
 searched also in implementation class. For this reason only managed classes of types that require implementation may have
 annotations, see {@link InstanceType#requiresImplementation()}.
 <p>
 Optionally, managed class supports an extended annotations searching scope, something resembling JAX-RS, annotation
 inheritance. If annotation is not found in implementation class it is searched in super class or implemented interface.
 <p>
 For your convenience here is the list of supported annotations. For details see annotations class description.
 <table summary="Annotations List">
 <tr>
 <th>Annotation Class
 <th>Arguments
 <th>Description
 <th>Type
 <th>Method
 <th>Field
 <tr>
 <td>{@link Remote}
 <td>Class request URI path
 <td>Grant remote access from client logic to managed methods deployed in container.
 <td>true
 <td>true
 <td>false
 <tr>
 <td>{@link DenyAll}
 <td>N/A
 <td>Forbid remote access to particular methods inside a {@link Remote} accessible managed instance.
 <td>false
 <td>true
 <td>false
 <tr>
 <td>{@link PermitAll}
 <td>N/A
 <td>Remote accessible entity that do not require authorization.
 <td>true
 <td>true
 <td>false
 <tr>
 <td>{@link Singleton}
 <td>Class request URI path
 <td>Configure managed instance with {@link InstanceScope#APPLICATION}.
 <td>true
 <td>false
 <td>false
 <tr>
 <td>{@link Startup}
 <td>N/A
 <td>Private remote accessible managed methods, that cannot be invoked without authorization.
 <td>false
 <td>true
 <td>false
 <tr>
 <td>{@link Stateless}
 <td>Class request URI path
 <td>Configure managed instance with {@link InstanceScope#LOCAL}.
 <td>true
 <td>false
 <td>false
 <tr>
 <td>{@link Stateful}
 <td>N/A
 <td>Configure managed instance with {@link InstanceScope#SESSION}.
 <td>false
 <td>true
 <td>false
 <tr>
 <td>{@link Path}
 <td>Method request URI path
 <td>Managed method binding to particular resource path.
 <td>false
 <td>true
 <td>false
 <tr>
 <td>{@link Transactional}
 <td>N/A
 <td>Execute managed method into transactional scope.
 <td>true
 <td>true
 <td>false
 <tr>
 <td>{@link Immutable}
 <td>N/A
 <td>Immutable, that is, read-only transaction.
 <td>true
 <td>true
 <td>false
 <tr>
 <td>{@link Mutable}
 <td>N/A
 <td>Force mutable transaction on particular method inside a {@link Immutable} managed instance.
 <td>false
 <td>true
 <td>false
 <tr>
 <td>{@link Asynchronous}
 <td>N/A
 <td>Execute managed method in a separated thread of execution.
 <td>true
 <td>true
 <td>false
 <tr>
 <td>{@link Inject}
 <td>N/A
 <td>Managed instance field value injection.
 <td>false
 <td>false
 <td>true
 <tr>
 <td>{@link Interceptors}
 <td>interceptor class
 <td>An intercepted managed method executes an interceptor cross-cutting logic whenever is invoked.
 <td>true
 <td>true
 <td>false
 </table>
