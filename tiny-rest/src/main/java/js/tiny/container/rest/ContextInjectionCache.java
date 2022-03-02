package js.tiny.container.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContextInjectionCache {
	private final Map<Class<?>, List<IMemberInjector>> cache = new HashMap<>();

	public void add(Class<?> implementationClass, IMemberInjector injector) {
		List<IMemberInjector> injectors = cache.get(implementationClass);
		if (injectors == null) {
			injectors = new ArrayList<>();
			cache.put(implementationClass, injectors);
		}
		injectors.add(injector);
	}

	public List<IMemberInjector> get(Class<?> implementationClass) {
		List<IMemberInjector> injector = cache.get(implementationClass);
		return injector != null ? injector : Collections.emptyList();
	}
}
