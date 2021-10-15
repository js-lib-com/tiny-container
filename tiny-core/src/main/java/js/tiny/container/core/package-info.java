/**
 * Container with inversion of control, dependency injection and declarative services.  
 * This library core package. Contains server master and application factories, logic for managed classes, methods and remote methods 
 * with support for managed life cycle, external configuration and interceptors, resource methods to resource path bindings and 
 * unchecked runtime exceptions.
 * <p>
 * A managed class is a standard Java class declared as managed into application descriptor. Into application descriptor there is a
 * predefined section named <code>managed-classes</code> where all managed classes are declared, a class per child element. For a
 * discussion about managed class configuration please see {@link js.tiny.container.spi.IManagedClass} description.
 * <pre>
 * &lt;managed-classes&gt;
 *  &lt;demo interface="comp.prj.Demo" implementation="comp.prj.DemoImpl" type="POJO" scope="SESSION" /&gt;
 *    . . .
 * &lt;/managed-classes&gt;
 * </pre>
 * As result from above configuration snippet every managed class has an interface, an implementation, a type and a scope. Is acceptable,
 * though not really good practice, to use only <code>class</code> instead of <code>interface</code>/<code>implementation</code> tuple. 
 * There are two major types of managed classes: {@link js.tiny.container.core.InstanceType#POJO} and {@link js.tiny.container.core.InstanceType#PROXY}. Only container can 
 * benefit from all managed classes services, see list below. 
 * <p>
 * A managed class is not created with <code>new</code> operator; there is a {@link js.tiny.container.core.AppFactory factory} dedicated exactly for 
 * that. Every application has its own managed instances factory; there is also a master factory - see {@link js.tiny.container.core.Factory}, with 
 * server global visibility. Every HTTP request is addressed to an application in a specific execution thread; master factory uses request 
 * this thread to store application specific factory so that is able to the delegate correct factory no matter from which application is 
 * used. Anyway, a managed class should first register to application factory; this happens on application factory creation, when all 
 * managed classes are created based on information from application descriptor. 
 * <p>
 * Managed instances are created by managed instances {@link js.tiny.container.core.AppFactory factory} and comes in two flavors: managed POJOs and 
 * managed containers. Managed containers are in fact Java Proxy, with bytecode generated dynamically and allows for method level,
 * cross-cutting services like declarative transactions. Managed POJOs are more lightweight but no method level services. Here 
 * are listed all services supplied by current implementation:
 * <ol>
 * <li>Class level services, available to all managed classes:
 * <ul>
 * <li>Declarative implementation binding.
 * <li>Life span management for managed class.
 * <li>Managed {@link js.lang.ManagedLifeCycle life cycle} for managed classes with server and application scopes.
 * <li>{@link js.tiny.container.annotation.Inject Dependency} injection for fields and constructor parameters.
 * <li>External configuration of managed instance fields from application descriptor.
 * <li>Managed classes static fields initialization from application descriptor.
 * </ul>
 * <li>Method level services, for POJO methods declared as remote or all containers methods:
 * <ul>
 * <li>Authenticated remote access to {@link js.tiny.container.annotation.Remote remote} managed methods; authentication occurs at method invocation.
 * <li>Declarative {@link js.tiny.container.annotation.Asynchronous asynchronous} execution mode for long running logic, executed in separated thread.
 * <li>Method invocation listener. There are {@link js.tiny.container.interceptor.Interceptor interceptors} for before, after and around method invocation.
 * <li>Method instrumentation. Uses {@link js.tiny.container.perfmon.IInvocationMeter} to monitor method invocations.
 * </ul>
 * <li>Container level services:
 * <ul>
 * <li>Declarative {@link js.annotation.Transactional transactions}, both mutable and immutable.
 * </ul>
 * </ol>
 * Any managed method declared {@link js.tiny.container.annotation.Remote remote} can be invoked remotely via <a href="../rmi/package-summary.html#http-rmi">HTTP-RMI</a> 
 * protocol; remote invocation follows Java reflection paradigm but uses managed entities instead:
 * <pre id="remote-sample-code">
 * JsClass&lt;?&gt; jsClass = jsFactory.getRemoteAccessibleJsClass(className);
 * JsRemoteMethod jsMethod = jsClass.getRemoteAccessibleJsMethod(methodName);
 * Object[] parameters = getParameters(jsMethod.getParameterTypes());
 * Object instance = jsFactory.getInstance(jsClass);
 * Object returnValue = jsMethod.invoke(instance, parameters);
 * </pre>
 * 
 * <h3>Load Services</h3>
 * Tiny Container supports loading service instances for a given service interface. This mechanism allows for implementation
 * selection at run-time based on deployed service implementation archive.
 * <p>
 * Implementation archive should include in META-INF/services/ directory a file with the name equals with service interface
 * qualified class name and content implementation qualified class name. For example, for <code>net.dots.gpio.GPIO</code>
 * service interface and implementation <code>net.dots.gpio.win.GpioImpl</code>, archive should contain the file
 * <code>META-INF/services/net.dots.gpio.GPIO</code> with content <code>net.dots.gpio.win.GpioImpl</code>. To generate service
 * implementation file from Ant uses <code>service</code> nested element from <code>jar</code> task, see below snippet.
 * 
 * <pre>
 * &lt;jar destfile="build/win-sys.jar"&gt;
 *     &lt;zipfileset dir="bin" /&gt;
 *     &lt;service type="net.dots.gpio.GPIO" provider="net.dots.gpio.win.GpioImpl" /&gt;
 * &lt;/jar&gt;
 * </pre>
 * 
 * An alternative method would be to create above META-INF/services directory under source base directory and include in
 * binaries archive.
 * 
 * <h3>Remote Instances</h3>
 * 
 * @author Iulian Rotaru
 */
package js.tiny.container.core;