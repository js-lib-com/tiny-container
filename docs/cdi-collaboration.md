# CDI Collaboration

Injector keeps track of registered bindings. A binding  is a mapping between an instance key and a provider.  Binding are loaded at injector creation.

There two kinds of providers: provisioning and scope providers. Provisioning providers are those that actually create instances. Scope providers uses a cache for instance reuse , giving instances a certain life span.

Provisioning providers generates events when create a new instance. To catch provision events one should create a `ProvisionListener`.



![](D:\docs\workspaces\js-lib\wtf\container\tiny-container\docs\cdi-collaboration.png)



Ultimately, when business method ask container to create a new instance, container invoke a provider from injector. But container need to know when a new instance is created so that to run instance post construction processor.