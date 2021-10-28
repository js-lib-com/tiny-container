package com.jslib.injector;

import java.lang.annotation.Annotation;

/**
 * Injector facade deals with bindings configuration, instances creation and provisioning events.
 * 
 * @author Iulian Rotaru
 */
public interface IInjector {

	void configure(IModule... modules);
	
	<T> IBindingBuilder<T> getBindingBuilder(Class<T> type);
	
	<T, I extends T> void bindInstance(Key<T> key, I instance);
	
	<T> T getInstance(Class<T> type);

	<T> T getInstance(Key<T> key);

	<T> T getInstance(Class<T> type, String name);
	
	<T> T getScopeInstance(Class<T> type);
	
	<T> void bindListener(IProvisionListener<T> provisionListener);
	
	<T> void unbindListener(IProvisionListener<T> provisionListener);
	
	<T> void fireEvent(IProvisionInvocation<T> provisionInvocation);

	void bindScope(Class<? extends Annotation> annotation, IScope scope);
	
	IScope getScope(Class<? extends Annotation> annotation);

}
