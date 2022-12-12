package com.jslib.container.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * A parameter from a managed method. It has a type and optional annotations.
 * 
 * Parent managed method is declared into a managed class that can have both interface and implementation classes. Managed
 * parameter implementation should scan for parameter annotations on both methods, that is, from method declared on managed
 * interface class and on managed implementation class, if the case.
 * 
 * @author Iulian Rotaru
 */
public interface IManagedParameter {

	Type getType();

	Annotation[] getAnnotations();

	<A extends Annotation> A scanAnnotation(Class<A> annotationClass);

}
