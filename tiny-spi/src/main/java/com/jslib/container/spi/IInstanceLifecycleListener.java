package com.jslib.container.spi;

/**
 * Listener for instance lifecycle events. This listener is signaled when a new instance is created by injector and when a
 * scoped instance is out of scope.
 * 
 * Event for <code>new instance created</code> are usable for all instances created by injector and should be triggered strictly
 * for newly created instances but not when instance is reused from a scope cache.
 * 
 * A scoped instance is one created by a scoped provider. A scoped provider has some sort of cache and reuse an instance while
 * program flow is on scope. Just before program flow leaving a scope, <code>instance out of scope</code> event should be
 * triggered for all active instances, related to that scope.
 * 
 * 
 * @author Iulian Rotaru
 */
public interface IInstanceLifecycleListener {

	void onInstanceCreated(Object instance);

	void onInstanceOutOfScope(Object instance);

}
