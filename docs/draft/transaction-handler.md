# Transaction Handler

Invocation handler implementing container services for managed classes of {@link InstanceType#PROXY} type. Managed classes declared as {@link InstanceType#PROXY} have the actual managed instance wrapped in a Java Proxy, dynamically generated. That proxy routes all managed instance method invocations to this invocation handler {@link #invoke(Object, Method, Object[])} method that, on its turn, delegates wrapped instance. Since all invocations pass through a single point is possible to add cross-cutting functionality like transactions.

Current version implements directly only logic related to transactional execution and delegates {@link ManagedMethod} for method level service.

## Transactions

This class supports declarative transactions, both mutable and immutable. It uses {@link TransactionManager} to create a new transaction, invoke managed instance method and commit transaction; on any exception performs rollback. This class uses services provided by external implementation via {@link TransactionManager} interface and cannot work if there is no service provider on run-time. Anyway, {@link TransactionalResource}, that is injected by constructor, does detect transactional service provider and throws exception if not found.

![](D:\docs\workspaces\js-lib\wtf\container\tiny-container\docs\transaction-handler.png)   


Here is transaction handling algorithm implemented by managed proxy handler.

- ManagedProxyHandler invokes createTransaction on TransactionResource
- TransactionalResource delegates transaction creation to TransactionManager
- TransactionManager creates transaction and session object
- TransactionManager returns transaction that contains session object
- ManagedProxyHandler stores session object on TransactionalResource
- ManagedProxyHandler uses transaction instance to control transaction life cycle
- ManagedProxyHandler invokes service method
- service method uses TransactionalResource to retrieve session object
- ManagedProxyHandler close transaction and release session from TransactionalResource
