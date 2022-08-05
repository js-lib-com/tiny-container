package com.jslib.container.resource;

import java.lang.reflect.Field;
import java.util.Collection;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.container.spi.IInstancePostConstructProcessor;

import jakarta.annotation.Resource;

import com.jslib.util.Classes;
import com.jslib.util.Strings;

/**
 * Post processor for instance resources injection. Remember that a resource is a service external to container for which there
 * is a connector. Also term <code>resource</code> refers to simple environment entries, as defined by EJB specification. This
 * processor injects both resource objects and simple environment entries but is not allowed to inject resources into static
 * fields.
 * 
 * In essence this processor scans for {@link Resource} annotation on managed classes, and for every field retrieves its value
 * from JNDI and injects it reflexively. Note that only fields from managed class implementation are scanned but not super
 * classes.
 * 
 * {@link Resource} annotation has two means to retrieve objects from JNDI: {@link Resource#lookup()} and
 * {@link Resource#name()}. For <code>lookup</code> this implementation uses global environment <code>java:global/env</code>
 * whereas for <code>name</code> uses component relative environment, <code>java:comp/env</code>. If {@link Resource} annotation
 * has no attribute uses class canonical name followed by field name, separated by slash ('/').
 * 
 * @author Iulian Rotaru
 */
public class ResourcesInjectionProcessor implements IInstancePostConstructProcessor {
	private static final Log log = LogFactory.getLog(ResourcesInjectionProcessor.class);

	private static final String GLOBAL_ENV = "java:global/env";
	private static final String COMP_ENV = "java:comp/env";

	private final Context rootContext;
	private final Context globalEnvironment;
	private final Context componentEnvironment;
	private final FieldsCache fieldsCache;

	public ResourcesInjectionProcessor() {
		this.rootContext = context();
		this.globalEnvironment = context(rootContext, GLOBAL_ENV);
		this.componentEnvironment = context(rootContext, COMP_ENV);
		this.fieldsCache = new FieldsCache();
	}

	private static Context context() {
		try {
			return new InitialContext();
		} catch (NamingException e) {
			log.error("Fail to create JNDI initial context. Root cause: %s: %s", e.getClass().getCanonicalName(), e.getMessage());
			return null;
		}
	}

	private static Context context(Context rootContext, String contextName) {
		if (rootContext == null) {
			return null;
		}
		Context context = null;
		try {
			context = (Context) rootContext.lookup(contextName);
		} catch (NamingException e) {
			log.error("JNDI context |%s| lookup fail. Root cause: %s: %s", contextName, e.getClass().getCanonicalName(), e.getMessage());
		}
		return context;
	}

	/** Test constructor. */
	ResourcesInjectionProcessor(Context globalEnvironment, Context componentEnvironment, FieldsCache fieldsCache) {
		this.rootContext = null;
		this.globalEnvironment = globalEnvironment;
		this.componentEnvironment = componentEnvironment;
		this.fieldsCache = fieldsCache;
	}

	@Override
	public Priority getPriority() {
		return Priority.INJECT;
	}

	/**
	 * Initialize resource fields from managed class with value retrieved from JNDI.
	 * 
	 * @param managedClass managed class,
	 * @param instance instance of given managed class.
	 */
	@Override
	public <T> void onInstancePostConstruct(T instance) {
		if (instance == null) {
			// null instance is silently ignored
			return;
		}

		fieldsCache.get(instance.getClass()).forEach(field -> {
			Object value = getJndiValue(field);
			if (value == null) {
				log.debug("Null dependency for field |%s|. Leave it unchanged.", field);
				return;
			}
			Classes.setFieldValue(instance, field, value);
			log.debug("Inject field |%s| value |%s|.", field, value);
		});
	}

	/**
	 * Load JNDI object or simple environment entry identified by field {@link Resource} annotation. Returns null if JNDI value
	 * is not found.
	 * 
	 * Provided class field is guaranteed to have {@link Resource} annotation. {@link Resource} annotation has two means to
	 * retrieve objects from JNDI: {@link Resource#lookup()} and {@link Resource#name()}. For <code>lookup</code> this
	 * implementation uses global environment <code>java:global/env</code> whereas for <code>name</code> uses component relative
	 * environment, <code>java:comp/env</code>.
	 * 
	 * If {@link Resource} annotation has no attribute infer resource <code>name</code> from class field: uses declaring class
	 * canonical name followed by field name, separated by slash ('/').
	 * 
	 * @param field class field, guaranteed to have {@link Resource} annotation.
	 * @return JNDI object or simple environment entry or null if not found.
	 */
	private Object getJndiValue(Field field) {
		Resource resource = field.getAnnotation(Resource.class);

		String lookupName = resource.lookup();
		if (!lookupName.isEmpty()) {
			return jndiLookup(globalEnvironment, GLOBAL_ENV, lookupName);
		}

		String name = resource.name();
		if (name.isEmpty()) {
			name = Strings.concat(field.getDeclaringClass().getName(), '/', field.getName());
		}
		if (name.startsWith("java:")) {
			throw new IllegalArgumentException("Resource name should be relative to component environment: " + name);
		}
		return jndiLookup(componentEnvironment, COMP_ENV, name);
	}

	/**
	 * Perform JNDI object lookup on given naming context. If naming context is null this method silently returns null.
	 * 
	 * @param namingContext naming context, possible null,
	 * @param contextName context name, for logging,
	 * @param name JNDI object name relative to naming context.
	 * @return JNDI object or null if not found.
	 */
	private static Object jndiLookup(Context namingContext, String contextName, String name) {
		if (namingContext == null) {
			return null;
		}

		Object value = null;
		try {
			value = namingContext.lookup(name);
			log.info("Load JDNI object |%s/%s| of type |%s|.", contextName, name, value.getClass());
		} catch (NamingException e) {
			log.warn("Missing JNDI object |%s/%s|.", contextName, name);
		}
		return value;
	}

	// --------------------------------------------------------------------------------------------
	// tests access

	Collection<Field> getManagedFields(Class<?> implementationClass) {
		return fieldsCache.get(implementationClass);
	}
}
