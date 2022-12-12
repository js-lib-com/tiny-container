package com.jslib.container.ejb;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;

/**
 * Cache for fields annotated with {@literal}EJB annotation. Cache key is the class of the instance containing annotated fields.
 * Since it is legal to have multiple fields annotated with {@literal}EJB annotation in the same class, internal cache value is
 * a collection. This collection is returned when retrieve cached fields, see {@link #getFields(Class)}.
 * 
 * @author Iulian Rotaru
 */
class FieldsCache {
	private static final Log log = LogFactory.getLog(FieldsCache.class);

	/**
	 * Cache for class fields annotated with {@literal}EJB. Cache key is the class of the instance containing annotated fields.
	 * Since it is legal to have multiple fields annotated with {@literal}EJB in the same class, cache value is a set.
	 */
	private final Map<Class<?>, Set<Field>> cache;

	public FieldsCache() {
		this.cache = new HashMap<>();
	}

	/**
	 * Add field to cache if it is not already cached. If field parameter is already cached this method silently does nothing.
	 * 
	 * @param instanceClass cache key is the class of the instance containing annotated field,
	 * @param field field annotated with {@literal}EJB annotation.
	 */
	public void addField(Class<?> instanceClass, Field field) {
		Set<Field> fields = cache.get(instanceClass);
		if (fields == null) {
			synchronized (this) {
				fields = cache.get(instanceClass);
				if (fields == null) {
					fields = new HashSet<>();
					cache.put(instanceClass, fields);
				}
			}
		}
		if (fields.add(field)) {
			log.debug("Cache EJB field {}.", field);
		}
	}

	/**
	 * Return annotated fields collection of the requested instance class. Return empty collection if there is no cached fields
	 * for given instance class. Instance class is used as cache key and should match {@link #addField(Class, Field)} key.
	 * 
	 * @param instanceClass the class to return annotated fields for, used as cache key.
	 * @return annotated fields collection, possible empty but never null.
	 */
	public Collection<Field> getFields(Class<?> instanceClass) {
		return cache.getOrDefault(instanceClass, Collections.emptySet());
	}
}
