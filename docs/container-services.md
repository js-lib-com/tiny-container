# Container Services

Application developer is not required to be an expert at system-level programming; usually does not program transactions, concurrency, security, distribution, or other services into the application logic. Application developer relies on the container for these services.

At the bootstrap time, the container scans the classpath and creates metadata about the discovered classes. The metadata may take the form of metadata annotations applied to the managed classes and/or an external XML deployment descriptor. The metadata, no matter how expressed, includes the structural information of the managed class and declares all the external dependencies.

![](D:\docs\workspaces\js-lib\wtf\container\tiny-container\docs\container-services.png)



A resource is a dependency that is outside container, for example a database. Anyway, resource connectors are deployed on container classpath so that container can load them. 

There are two kinds of dependencies injection: on new instance and on already created instance. First combine instance creation and constructor dependencies injection. The second perform injection on instance fields and methods. For this reason is not possible to have separated factory; injector is indeed a factory itself. Now, since injector is a factory it should deal with instance scope too but the only mandatory scope is the singleton. Extension scopes may be provided by plugins.

## Class Services

| Service | Description |
|---------|-------------|
| Instance Factory | Create managed instances. |
| Instance Scope | A instance may have  application, thread, session or local scope. A new managed instance can be created at every request or can be reused from a cache, depending on managed class scope. Scope is declared on managed class declaration. New managed instance are created by {@link js.container.ManagedClass} but life span is controlled by {@link js.container.Container} class. |
| Dependency Injection | Current container supports two types of dependency injection: field injection using {@link js.container.annotation.Inject} annotation and constructor injection. Injected instance should be managed instance on its turn; is not possible to inject POJO instances. |
| Resource Injection | Resource injection via JNDI lookup. This include both resource objects like data source instances and simple environment entries. |
| Bean Instance Pool | automatically increase and reduce the number of available bean instances so that there is not much load on the current server. |
| Managed Life Cycle | For managed instances implementing {@link js.lang.ManagedLifeCycle} interface container invokes post-create and pre-destroy methods after managed instance creation, respective before destroying. Managed life cycle is supported only by managed classes with application scope. |

## Method Services

Container services enacted before and / or after method invocation.

| Service | Description |
|---------|-------------|
| Local Transactions | A business method can be declared as transactional. Container should execute method inside transaction boundaries. |
| Asynchronous Execution | If a business method is declared asynchronous container creates a separated thread and return immediately. Asynchronous method should return void. |
| Timer and Scheduler | set up a timer that invokes a timeout callback method at a specified time, after a specified elapsed time, or at specified intervals. |
| Invocation Interceptors | There are interceptors for before, after and around method invocation. Invocation listeners provide a naive, but still useful AOP. There is {@link js.container.annotation.Intercepted} annotation for tagging methods - declaring join points, and related interface to be implemented by interceptors, aka AOP advice. See {@link js.container.Interceptor} interface for sample usage. |
| Method Instrumentation | Uses {@link js.container.InvocationMeter} to monitor method invocations. Every managed method has a meter that updates internal counters about execution time, invocation and exceptions count. Invocation meter interface is used to collect counter values. Instrumentation manager, {@link js.container.Observer}, collects periodically all managed methods counters and create report on system logger. |

Known limitations:

1. No support for distributed transactions. 

## Connector Services

| Service | Description |
|---------|-------------|
| Remote Access | A method annotated with, or owned by a class annotated with {@link js.container.annotation.Remote} is named net method and is accessible from remote. Remote access is controller by {@link js.container.annotation.Public} and {@link js.container.annotation.Private} annotations. A private net method can be accessed only after authentication. |
| Local Access | Access from the same JVM but not from the same application context, e.g. different webapp deployed on the same Tomcat instance. |
| Web Service | REST access |
