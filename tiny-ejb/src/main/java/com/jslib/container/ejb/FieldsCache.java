package com.jslib.container.ejb;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;

import com.jslib.util.Strings;

public class FieldsCache {
	private static final Log log = LogFactory.getLog(FieldsCache.class);

	private static final String COMP_ENV = "java:comp/env";

	/** Cache for class fields annotated with {@literal}EJB. */
	private final Map<Class<?>, Set<EjbField>> cache = new HashMap<>();

	private final Context context;

	public FieldsCache() throws NamingException {
		Context root = new InitialContext();
		context = (Context) root.lookup(COMP_ENV);
	}

	public Collection<EjbField> get(Class<?> implementationClass) {
		return cache.getOrDefault(implementationClass, Collections.emptySet());
	}

	public void add(Class<?> implementationClass, Field field) {
		Set<EjbField> ejbFields = cache.get(implementationClass);
		if (ejbFields == null) {
			synchronized (this) {
				if (ejbFields == null) {
					ejbFields = new HashSet<>();
					cache.put(implementationClass, ejbFields);
				}
			}
		}

		String ejbUrl = Strings.concat(field.getType().getCanonicalName(), ".url");
		try {
			String implementationURL = (String) context.lookup(ejbUrl);
			EjbField ejbField = new EjbField(field, implementationURL);
			ejbFields.add(ejbField);
			log.debug("Discover EJB |{ejb_class}| on field |{java_type}#{java_field}|.", ejbField, implementationClass.getCanonicalName(), field.getName());
		} catch (NamingException e) {
			log.error("JNDI lookup fail on EJB |{ejb_class}|. Root cause: {exception_class}: {exception_message}", ejbUrl, e.getClass().getCanonicalName(), e.getMessage());
		}
	}
}
