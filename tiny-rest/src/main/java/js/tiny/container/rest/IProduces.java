package js.tiny.container.rest;

import js.tiny.container.spi.IManagedMethod;

/**
 * Meta interface for both Jakarta and Java <code>ws.rs.Produces</code> annotations.
 * 
 * @author Iulian Rotaru
 */
interface IProduces {

	String[] value();

	static IProduces scan(IManagedMethod managedMethod) {
		jakarta.ws.rs.Produces jakartaProduces = managedMethod.scanAnnotation(jakarta.ws.rs.Produces.class, IManagedMethod.Flags.INCLUDE_TYPES);
		if (jakartaProduces != null) {
			return () -> jakartaProduces.value();
		}

		javax.ws.rs.Produces javaxProduces = managedMethod.scanAnnotation(javax.ws.rs.Produces.class, IManagedMethod.Flags.INCLUDE_TYPES);
		if (javaxProduces != null) {
			return () -> javaxProduces.value();
		}

		return null;
	}
}
