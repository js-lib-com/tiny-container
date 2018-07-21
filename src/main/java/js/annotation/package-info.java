/**
 * Annotations for container declarative services. This package supplies annotations for remote access 
 * with authorization control, declarative mutable and immutable transactions, declarative method asynchronous 
 * execution, fields dependency injection, managed method invocation interceptor and resource method 
 * binding. Since provided annotations are processed on deployment phase they all have run-time retention. 
 * All annotation operates on managed instances properties and couple of them on interfaces or classes. 
 * If applied on type all methods inherit that annotation. Also, type targeted annotation should be 
 * applied to managed class interface and only if interface is missing to actual class.   
 *
 * @author Iulian Rotaru
 * @version draft
 */
package js.annotation;