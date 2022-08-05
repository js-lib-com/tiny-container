package com.jslib.container.cdi;

/**
 * Simplified bindings interface for managed classes.
 * 
 * @author Iulian Rotaru
 */
public interface IClassBinding<T> {

	Class<T> getInterfaceClass();

	Class<? extends T> getImplementationClass();

}