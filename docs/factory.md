# Managed instances factory

Managed instances factory with application scope. A managed instance is one created from a managed class. Managed instances
are not created with Java new operator but handled by this factory based on properties from managed class. There is a single
application factory implementation per application. Into web contexts, where multiple applications can be deployed, there is
an application factory for each web application instance.

This interface is the main front-end for managed instances <code>retrieval</code>. It is named <code>retrieval</code> and not
<code>creation</code> because an instance is not always created. It can be reused from scope caches, loaded using standard
Java service loader or proxied to a remote class, depending on managed class scope. To retrieve a managed instance one should
know its registered interface. It is also possible to have multiple instances of the same interface in which case caller
should provide the instance name. Is is considered a bug if supplied interface is not registered. Registration is
implementation detail but it should be able to locate instances based on interface class and optional instance name.

Main factory method is {@link #getInstance(Class)} and its named variant {@link #getInstance(String, Class)}. They always
return managed instances, reused or fresh created but never null. Application is not required to test returned value but if
managed instance retrieval fails unchecked exception or error is thrown. Failing conditions are almost exclusively
development or deployment mistakes. For a sound deployment, on production only out of memory could happen.

Mentioned factory methods fail if requested managed class is not registered or a service provider is not found. If this
behavior is not desirable there is {@link #getOptionalInstance(Class)} that returns null in such conditions. It is
application responsibility to check returned value and take recovery measures.

Application factory is able to retrieve instances for remotely deployed classes. In this case managed class type is declared
to be <code>REMOTE</code> and factory implementation creates a Java Proxy that knows HTTP-RMI. This proxy is returned by
above factory methods and application can invoke methods directly on it, as if would be local instance. A managed class
declared as remote should have a property that configure URL where remote class is deployed. If this URL is missing
implementation should perform discovery.

For special case when remote class URL is obtained at run-time, perhaps from user interface, there is
{@link #getRemoteInstance(String, Class)} that retrieve a remote instance for specified URL.
