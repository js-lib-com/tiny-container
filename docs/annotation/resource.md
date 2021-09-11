# javax.annotation.Resource

It is part of JSE.



Not clear if need to use `name` or `lookup` and if value should include environment context `java:comp/env` or `java:global/env`.

possible: name use default `java:comp/env` whereas lookup uses `java:global/env` ???



`java:comp` - current component naming context

`java:global` - application server global naming context; `java:comp` is part of `java:global`



Tomcat require full JDNI path when use lookup; it seems supported by API doc:  ... It can link to any compatible resource using the global JNDI names. In this case I presume global means absolute name, starting with java: root context.

Tomcat throws exception if name is absolute, complaining about invalid character ':'.

------



Tiny Container current implementation does not use `lookup` and default `name` is `${class-name}/${field-name}`. Name is always searched on component environment naming context - `java:comp/env`.

