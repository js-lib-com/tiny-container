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
