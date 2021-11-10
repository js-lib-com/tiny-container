# javax.annotation.Resource

`@Resource` annotation is part of the JSE and used to inject resources into instances fields; static fields are not supported. Remember that a resource is a service external to container for which there is an adapter. Adapter API is the only means to access resource services.

Also term `resource` refers to simple environment entries, as defined by EJB specification. This module injects both resource adapters and simple environment entries but.

`@Resource` annotation has two means to retrieve objects from JNDI: `lookup` and `name`. For `lookup` this implementation uses global environment `java:global/env` whereas for `name` uses component relative environment, `java:comp/env`. If `@Resource` annotation has no attribute uses class canonical name followed by field name, separated by slash ('/') - `${canonical-class-name}/${field-name}`.



## Component Simple Environment Entry

JNDI lookup name is `java:comp/env/service.name`.

```java
@Resource(name = "service.name")
private String name;
```



Simple environment entries declaration depends on runtime implementation. As an example here is Tomcat context.

```xml
<Context reloadable="true">
	<Environment name="service.name" type="java.lang.String" value="Service Name" />
</Context>

```



## Global Simple Environment Entry

JNDI lookup name is `java:global/env/service.name`.


```java
@Resource(lookup = "service.name")
private String name;
```



Global resources are declared on `server.xml`.

```xml
<GlobalNamingResources>
	<Environment name="service.name" type="java.lang.String" value="Service Name" />
</GlobalNamingResources>
```

