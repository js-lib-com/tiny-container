# Container Services

| Service | Description |
|---------|-------------|
| Dependency injection | Current container supports two types of dependency injection: field injection using {@link js.container.annotation.Inject} annotation and constructor injection. Injected instance should be managed instance on its turn; is not possible to inject POJO instances. |
| Bean Instance Pool | automatically increase and reduce the number of available bean instances so that there is not much load on the current server. |
| Instance scope management | A instance may have  application, thread, session or local scope. A new managed instance can be created at every request or can be reused from a cache, depending on managed class scope. Scope is declared on managed class declaration. New managed instance are created by {@link js.container.ManagedClass} but life span is controlled by {@link js.container.Container} class. |
| Managed life cycle | For managed instances implementing {@link js.lang.ManagedLifeCycle} interface container invokes post-create and pre-destroy methods after managed instance creation, respective before destroying. Managed life cycle is supported only by managed classes with application scope. |
| Resource injection | Resource injection via JNDI lookup. This include both resource objects like data source instances and simple environment entries. |
| Local transactions | A managed class can be marked as transactional, see {@link js.container.annotation.Transactional} annotation. Container creates a Java Proxy handler and execute managed method inside transaction boundaries. |
| Disributed transactions | Transaction on two or more remote transactional resources. |
| Authenticated remote access | A method annotated with, or owned by a class annotated with {@link js.container.annotation.Remote} is named net method and is accessible from remote. Remote access is controller by {@link js.container.annotation.Public} and {@link js.container.annotation.Private} annotations. A private net method can be accessed only after authentication. |
| Web service | REST access |
| Declarative asynchronous execution | If a managed method is annotated with {@link js.container.annotation.Asynchronous} container creates a separated thread and execute method asynchronously. Asynchronous method should return void. |
| Timer and scheduler | set up a timer that invokes a timeout callback method at a specified time, after a specified elapsed time, or at specified intervals. |
| Method invocation interceptors | There are interceptors for before, after and around method invocation. Invocation listeners provide a naive, but still useful AOP. There is {@link js.container.annotation.Intercepted} annotation for tagging methods - declaring join points, and related interface to be implemented by interceptors, aka AOP advice. See {@link js.container.Interceptor} interface for sample usage. |
| Method instrumentation | Uses {@link js.container.InvocationMeter} to monitor method invocations. Every managed method has a meter that updates internal counters about execution time, invocation and exceptions count. Invocation meter interface is used to collect counter values. Instrumentation manager, {@link js.container.Observer}, collects periodically all managed methods counters and create report on system logger. |
