# Container

Container creates managed classes and handle managed instances life cycle, that is, instances creation and caching. Basic container functionality is managed methods invocation, methods that implement application defined logic for dynamic resources  generation and services. One can view container as a bridge between servlets and application defined resources and services or as an environment in which an application runs.

At is core container deals with managed classes. Container has a pool of managed classes and every managed class has a pool of managed methods. Beside managed life cycle container provides a host of declarative services to managed instances. Anyway, Container class does not implement managed instances services by itself but delegates factories and processors, implemented by js.container package or as plug-ins.

<pre>
      +-------------------------------------------------------------------+
      | Tiny Container                                                    |
      |    +------------+                              +---------------+  | 
      |    | Interfaces |                          +---&gt; ManagedMethod |  |
      |    +-----^------+       +--------------+   |   +---------------+  |
      |          |          +---&gt; ManagedClass +---+                      |
      |          |          |   +--------------+   |   +---------------+  |
      |    +-----+------+   |                      +---&gt; ManagedMethod |  |
   ---O----&gt; Container  +---+                          +---------------+  |
      |    +------------+   |                                             |
      |                     ~                      +---...                |
      |                     |   +--------------+   |                      |
      |                     +---&gt; ManagedClass +---+                      |
      |                         +--------------+   |                      |
      +-------------------------------------------------------------------+
 </pre>

 Here is a quick list of services provided, actually orchestrated, by container:
 <dl>
 <dt>Declarative implementation binding for managed classes.</dt>
 <dd>Managed class has interface used by caller to invoke methods on managed instance. Managed instance is create by factory instead of new operator. The actual implementation is declared into class descriptor and bound at container creation. Implementation bind cannot be hot changed.</dd>
<dt>Life span management for managed instances, e.g. application, thread or local scope.</dt>
<dd>A new managed instance can be created at every request or can be reused from a cache, depending on managed class scope. Instance scope is declared on managed class descriptor.</dd>
<dt>External configuration of managed instances from application descriptor.</dt>
<dd>If a managed instance implements {@link js.lang.Configurable} interface container takes care to configure it from managed class configuration section. Every managed class has an alias into application descriptor that identify configurable section. </dd>
<dt>Managed life cycle for managed instances.</dt>
<dd>For managed instances implementing {@link ManagedLifeCycle} interface container invokes post-create and pre-destroy methods after managed instance creation, respective before destroying.</dd>
<dt>Declarative transactions, both mutable and immutable.</dt>
<dd>A managed class can be marked as transactional, see {@link Transaction} annotation. Container creates a Java Proxy handler and execute managed method inside transaction boundaries.</dd>
<dt>Dependency injection for fields and constructor parameters.</dt>
<dd>Current container supports two types of dependency injection: field injection using {@link Inject} annotation and constructor injection. Injected instance should be managed instance on its turn or instantiable plain Java class.</dd>
<dt>Managed classes static fields initialization from application descriptor.</dt>
<dd>Inject value objects declared in application descriptor into static fields from managed class implementation. Container uses {@link Converter} to convert string to value type. Of course static field should not be final.</dd>
<dt>Authenticated remote access to managed methods; authentication occurs at method invocation.</dt>
<dd>A method annotated with, or owned by a class annotated with {@link Remote} is named net method and is accessible from remote. Remote access is controller by {@link PermitAll} and {@link Private} annotations. A private net method can be accessed only after authentication.</dd>
<dt>Declarative asynchronous execution mode for long running logic, executed in separated thread.</dt>
<dd>If a managed method is annotated with {@link Asynchronous} container creates a separated thread and execute method asynchronously. Asynchronous method should return void.</dd>
<dt>Method invocation listeners. There are interceptors for before, after and around method invocation.</dt>
<dd>Invocation listeners provide a naive, but still useful AOP. There is {@link Interceptors} annotation for tagging methods - declaring join points, and related interface to be implemented by interceptors, aka AOP advice. See {@link Interceptor} interface for sample usage.</dd>
<dt>Method instrumentation. Uses {@link IInvocationMeter} to monitor method invocations.</dt>
<dd>Every managed method has a meter that updates internal counters about execution time, invocation and exceptions count. Invocation meter interface is used to collect counter values. Instrumentation manager, {@link Observer}, collects periodically all managed methods counters and create report on server logger.</dd>
</dl>

## Descriptors
Every application has a descriptor for declarative services and state initialization. Application descriptor is a XML file, with root child elements named `sections`. Tiny Container uses these `sections` to group related configurations and here we will focus only on container specific ones.

Container adds supplementary semantics to application descriptor: a section element name is known as `alias` and this alias can be use to refer other section, known as `linked` section. There are predefined sections, like `managed-class` and user defined sections, that need to be declared as linked sections into a predefined section, for example `processor` section.

Here are predefined container descriptor sections.
| Name | Description | Linked Section |
|-----------|------------------|----------------------|
| managed-classes | Managed class descriptors. | Managed class configuration object. |
| pojo-classes | Plain Java classes static field initialization. | List of static fields and respective intial values. |
| converters | Declarative converters. |N/A |

In sample below there is a predefined section `managed-class` that has element `processor`. Element is a class descriptor and its name is related to linked section `processor`, section that is the managed class configuration object. For details about managed classes descriptors see {@link ManagedClass}.

<pre>
 &lt;managed-classes&gt;
 	&lt;processor interface="js.email.Processor" class="js.email.ProcessorImpl" scope="APPLICATION" /&gt;
 	...
 &lt;/managed-classes&gt;

 &lt;pojo-classes&gt;
 	&lt;email-reader class="com.mobile.EmailReader" /&gt;
 	...
 &lt;/pojo-classes&gt;

 &lt;processor repository="/var/www/vhosts/kids-cademy.com/email" files-pattern="*.htm" &gt;
 	&lt;property name="mail.transport.protocol" value="smtp" /&gt;
 	...
 &lt;/processor&gt;

 &lt;email-reader&gt;
 	&lt;static-field name="ACCOUNT" value="john.doe@email.com" /&gt;
 	...
 &lt;/email-reader&gt;

 &lt;converters&gt;
 	&lt;type class="js.email.MessageID" converter="js.email.MessageIDConverter" /&gt;
 	...
 &lt;/converters&gt;
</pre>

For `pojo-classes` and `converters` predefined sections see {@link #pojoStaticInitialization(Config)}, respective {@link #convertersInitialization(Config)}.

## Life Cycle

Container has a managed life cycle on its own. This means it is created automatically, initialized and configured, post-constructed and just before ending its life, pre-destroyed. Accordingly, container's life passes five phases:

1. Construct container - create factories and processors for instance management but leave classes pool loading for next step, see {@link #Container()},
2. Create managed classes pool - scans application and class descriptors and creates all managed classes described there, see {@link #config(Config)},
3. Start managed classes with managed life cycle - this step should occur after all managed classes were loaded and container is fully initialized, see {@link #start()}; this is because a managed instance may have dependencies on other managed classes for injection,
4. Container is running - this stage has no single entry point; it is the sum of all services container provide while is up and running,
5. Destroy container - destroy and release caches, factories and processors; this is container global clean-up invoked at application unload, see {@link #destroy()}.


## Instance Retrieval Algorithm

Instance retrieval is the process of obtaining a managed instance, be it from scope factories caches or fresh created by specialized instance factories.

1. obtain an instance key based on {@link IManagedClass#getKey()} or instance name,
2. use interface class to retrieve managed class from container classes pool,
3. if managed class not found exit with bug error; if {@link #getOptionalInstance(Class, Object...)} returns null,
4. get scope and instance factories for managed class instance scope and type; if managed class instance scope is local there is no scope factory,
5. if scope factory is null execute local instance factory, that creates a new instance, and returns it; applies arguments processing on local instance but not instance processors,
6. if there is scope factory do next steps into synchronized block,
7. try to retrieve instance from scope factory cache,
8. if no cached instance pre-process arguments, create a new instance using instance factory and persist instance on scope factory,
9. end synchronized block,
10. arguments pre-processing takes care to inject constructor dependencies,
11. if new instance is created execute instance post-processors into registration order.

---

 Container with inversion of control, dependency injection and declarative services.  
 This library core package. Contains server master and application factories, logic for managed classes, methods and remote methods 
 with support for managed life cycle, external configuration and interceptors, resource methods to resource path bindings and 
 unchecked runtime exceptions.
 <p>
 A managed class is a standard Java class declared as managed into application descriptor. Into application descriptor there is a
 predefined section named <code>managed-classes</code> where all managed classes are declared, a class per child element. For a
 discussion about managed class configuration please see {@link js.tiny.container.spi.IManagedClass} description.
 <pre>
 &lt;managed-classes&gt;
  &lt;demo interface="comp.prj.Demo" implementation="comp.prj.DemoImpl" type="POJO" scope="SESSION" /&gt;
    . . .
 &lt;/managed-classes&gt;
 </pre>
 As result from above configuration snippet every managed class has an interface, an implementation, a type and a scope. Is acceptable,
 though not really good practice, to use only <code>class</code> instead of <code>interface</code>/<code>implementation</code> tuple. 
 There are two major types of managed classes: {@link js.tiny.container.spi.InstanceType#POJO} and {@link js.tiny.container.spi.InstanceType#PROXY}. Only container can 
 benefit from all managed classes services, see list below. 
 <p>
 A managed class is not created with <code>new</code> operator; there is a {@link js.tiny.container.spi.IFactory factory} dedicated exactly for 
 that. Every application has its own managed instances factory; there is also a master factory - see {@link js.tiny.container.core.Factory}, with 
 server global visibility. Every HTTP request is addressed to an application in a specific execution thread; master factory uses request 
 this thread to store application specific factory so that is able to the delegate correct factory no matter from which application is 
 used. Anyway, a managed class should first register to application factory; this happens on application factory creation, when all 
 managed classes are created based on information from application descriptor. 
 <p>
 Managed instances are created by managed instances {@link js.tiny.container.spi.IFactory factory} and comes in two flavors: managed POJOs and 
 managed containers. Managed containers are in fact Java Proxy, with bytecode generated dynamically and allows for method level,
 cross-cutting services like declarative transactions. Managed POJOs are more lightweight but no method level services. Here 
 are listed all services supplied by current implementation:
 <ol>
 <li>Class level services, available to all managed classes:
 <ul>
 <li>Declarative implementation binding.
 <li>Life span management for managed class.
 <li>Managed {@link js.lang.ManagedLifeCycle life cycle} for managed classes with server and application scopes.
 <li>{@link js.tiny.container.annotation.Inject Dependency} injection for fields and constructor parameters.
 <li>External configuration of managed instance fields from application descriptor.
 <li>Managed classes static fields initialization from application descriptor.
 </ul>
 <li>Method level services, for POJO methods declared as remote or all containers methods:
 <ul>
 <li>Authenticated remote access to {@link js.tiny.container.annotation.Remote remote} managed methods; authentication occurs at method invocation.
 <li>Declarative {@link js.tiny.container.annotation.Asynchronous asynchronous} execution mode for long running logic, executed in separated thread.
 <li>Method invocation listener. There are {@link js.tiny.container.interceptor.Interceptor interceptors} for before, after and around method invocation.
 <li>Method instrumentation. Uses {@link js.tiny.container.perfmon.IInvocationMeter} to monitor method invocations.
 </ul>
 <li>Container level services:
 <ul>
 <li>Declarative {@link js.annotation.Transactional transactions}, both mutable and immutable.
 </ul>
 </ol>
 Any managed method declared {@link js.tiny.container.annotation.Remote remote} can be invoked remotely via <a href="../rmi/package-summary.html#http-rmi">HTTP-RMI</a> 
 protocol; remote invocation follows Java reflection paradigm but uses managed entities instead:
 <pre id="remote-sample-code">
 JsClass&lt;?&gt; jsClass = jsFactory.getRemoteAccessibleJsClass(className);
 JsRemoteMethod jsMethod = jsClass.getRemoteAccessibleJsMethod(methodName);
 Object[] parameters = getParameters(jsMethod.getParameterTypes());
 Object instance = jsFactory.getInstance(jsClass);
 Object returnValue = jsMethod.invoke(instance, parameters);
 </pre>

 <h3>Load Services</h3>
 Tiny Container supports loading service instances for a given service interface. This mechanism allows for implementation
 selection at run-time based on deployed service implementation archive.
 <p>
 Implementation archive should include in META-INF/services/ directory a file with the name equals with service interface
 qualified class name and content implementation qualified class name. For example, for <code>net.dots.gpio.GPIO</code>
 service interface and implementation <code>net.dots.gpio.win.GpioImpl</code>, archive should contain the file
 <code>META-INF/services/net.dots.gpio.GPIO</code> with content <code>net.dots.gpio.win.GpioImpl</code>. To generate service
 implementation file from Ant uses <code>service</code> nested element from <code>jar</code> task, see below snippet.

 <pre>
 &lt;jar destfile="build/win-sys.jar"&gt;
     &lt;zipfileset dir="bin" /&gt;
     &lt;service type="net.dots.gpio.GPIO" provider="net.dots.gpio.win.GpioImpl" /&gt;
 &lt;/jar&gt;
 </pre>

 An alternative method would be to create above META-INF/services directory under source base directory and include in
 binaries archive.

 <h3>Remote Instances</h3>

