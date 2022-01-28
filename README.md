# Tiny Container

Tiny Container is a light weight JEE compliant, modular container for solutions where size and code reuse matters. It is designed for backend business services consumed by smart clients like mobile and [PWA](https://en.wikipedia.org/wiki/Progressive_web_application) applications. Tiny Container intercepts calls for business services and add cross-cutting services; one can see it as an alternative to [AOP](https://en.wikipedia.org/wiki/Aspect-oriented_programming).

Usually business services are consumed by remote clients. For this, business services are exposed for remote access via built-in connectors for [HTTP-RMI](HTTP-RMI) and REST. Adding a new protocol is a matter to write a more or less simple connector.

Being so small Tiny Container is well suited for embedded systems with limited resources like [Raspberry Pi](https://www.raspberrypi.org/), micro-services or as wrapper to isolate business code from deployment runtimes: applications or web servers, cloud services or even embed into standalone applications.

Tiny Container is highly modular, its architecture resembling mico-kernel pattern. It has a core from couple classes and container service modules loaded at runtime with standard Java service loader.

- [GitHub Wiki](https://github.com/js-lib-com/tiny-container/wiki)
- [Javadoc IO](http://www.javadoc.io/doc/com.js-lib/tiny-container/1.0.0)
