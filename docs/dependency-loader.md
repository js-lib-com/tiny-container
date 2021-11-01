# Dependency Loader

 Base class for all processors dealing with dependencies. Supplies utility method for dependency instance retrieval with guard
 against circular dependencies. Usually {@link #getDependencyValue(IManagedClass, Class)} expect as dependency type a managed
 class and just delegate container for instance retrieval, see {@link IFactory#getOptionalInstance(Class, Object...)}. Anyway,
 if dependency type is not a managed class tries to instantiate it with standard {@link Class#newInstance()}; of course type
 should be concrete class and have default constructor. Otherwise throws bug error.

 Depending on host and dependency managed classes scope is possible that dependency value to be replaced by a scope proxy, see
 {@link ScopeProxyHandler}. This is to adapt dependency with shorted life span into host with larger life span; otherwise
 dependency may become invalid while host instance is still active. Logic to detect if scope proxy is required is encapsulated
 into separated utility method, see {@link #isProxyRequired(IManagedClass, IManagedClass)}.

## Circular Dependencies

 When container creates a new managed instance there will be a call to this class utility method. On its turn this class
 utility method may use container to retrieve instance for a dependency; this creates recursive calls that allows for
 unrestricted graph but is prone to circular dependencies.

 This class keeps a stack trace for classes in dependencies chain and throws bug error if discover a request for a dependency
 type that is already into stack trace. Since stack trace is kept on thread local storage circular dependencies guard works
 only in current thread.

 ## App Factory

 Application factory and its hierarchy cannot be managed classes since their job is to manage managed classes. If requested
 dependency type is a kind of application factory, container cannot be used for instance retrieval. This is <b>special</b>
 case handled by {@link #getDependencyValue(IManagedClass, Class)} with special logic: if dependency type is a kind of
 {@link IFactory} returns container reference provided by host managed class, see {@link IManagedClass#getContainer()}.

