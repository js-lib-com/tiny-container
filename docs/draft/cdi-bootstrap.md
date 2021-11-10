# CDI Bootstrap

Sequence of container and CDI creation, configuration and start.



![](D:\docs\workspaces\js-lib\wtf\container\tiny-container\docs\cdi-bootstrap.drawio.png)



1. Container creates CDI
2. Container loads services but only executes service state initialization
3. Container read application descriptor and create managed classes
4. Managed class scan services meta
5. Container configure CDI giving managed classes collection
6. CDI uses managed classes module to create injector bindings
7. CDI create injector with explicit bindings and managed classes modules
8. Container processors are executed on start

