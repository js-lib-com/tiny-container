package com.jslib.tiny.container.cdi;

import static java.lang.String.format;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Singleton;
import com.jslib.converter.Converter;
import com.jslib.converter.ConverterRegistry;
import com.jslib.api.injector.AbstractModule;
import com.jslib.api.injector.IBindingBuilder;
import com.jslib.lang.Config;
import com.jslib.lang.NoSuchBeingException;
import com.jslib.util.Classes;

/**
 * Injector module initialized from managed classes descriptor. This specialized module traverses descriptor elements, creating
 * injector bindings accordingly managed class instance type and scope. It is used only if application uses XML descriptor,
 * <code>app.xml</code>.
 * 
 * @author Iulian Rotaru
 */
class ConfigModule extends AbstractModule {

	private static final Map<String, Method> ATTRIBUTE_METHODS = new HashMap<>();
	static {
		ATTRIBUTE_METHODS.put("in", method("in", Class.class));
		ATTRIBUTE_METHODS.put("named", method("named", String.class));
		ATTRIBUTE_METHODS.put("on", method("on", String.class));
		ATTRIBUTE_METHODS.put("service", method("service"));
		ATTRIBUTE_METHODS.put("to", method("to", Class.class));
		ATTRIBUTE_METHODS.put("with", method("with", Class.class));
	}

	private static Method method(String name, Class<?>... parameterTypes) {
		try {
			return IBindingBuilder.class.getMethod(name, parameterTypes);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}

	private static final Map<String, Class<?>> SCOPE_CLASS_ALIASES = new HashMap<>();
	static {
		SCOPE_CLASS_ALIASES.put("Singleton", Singleton.class);
		SCOPE_CLASS_ALIASES.put("ApplicationScoped", ApplicationScoped.class);
		SCOPE_CLASS_ALIASES.put("SessionScoped", SessionScoped.class);
		SCOPE_CLASS_ALIASES.put("RequestScoped", RequestScoped.class);
	}

	private final Converter converter;
	private final Config config;
	private final String defaultPackage;

	public ConfigModule(Config config) {
		this.converter = ConverterRegistry.getConverter();
		this.config = config;

		String defaultPackage = config.getAttribute("package");
		this.defaultPackage = defaultPackage != null ? defaultPackage + "." : null;
	}

	@Override
	protected void configure() {
		config.getChildren().forEach(this::configure);
	}

	<T> void configure(Config bindingConfig) {
		String bindValue = bindingConfig.getAttribute("bind");
		if (bindValue == null) {
			throw new IllegalStateException("Missing <bind> attribute from binding definition.");
		}

		Class<T> bindClass = Classes.forName(classValue("bind", bindValue));
		IBindingBuilder<T> bindingBuilder = bind(bindClass);
		bindingConfig.attributes((name, value) -> bindAttribute(bindingBuilder, name, value));
	}

	void bindAttribute(IBindingBuilder<?> bindingBuilder, String name, String value) {
		if (name.equals("bind")) {
			return;
		}

		Method method = ATTRIBUTE_METHODS.get(name);
		if (method == null) {
			throw new IllegalStateException("Invalid attribute name " + name);
		}

		try {
			switch (method.getParameterCount()) {
			case 0:
				method.invoke(bindingBuilder);
				break;

			case 1:
				Class<?> parameterType = method.getParameterTypes()[0];
				if (parameterType.equals(Class.class)) {
					value = "in".equals(name) ? scopeValue(value) : classValue(name, value);
				}
				method.invoke(bindingBuilder, converter.asObject(value, parameterType));
				break;

			default:
				throw new IllegalStateException("You brought two too many!");
			}
		} catch (InvocationTargetException e) {
			throw new IllegalStateException(e.getTargetException().getMessage());
		} catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}

	private String scopeValue(String scopeValue) {
		boolean simpleName = scopeValue.indexOf('.') == -1;
		if (simpleName) {
			Class<?> scopeClass = SCOPE_CLASS_ALIASES.get(scopeValue);
			if (scopeClass == null) {
				throw new IllegalStateException(format("Bad scope value |%s| for attribute <in> on binding definition.", scopeValue));
			}
			return scopeClass.getCanonicalName();
		}
		return scopeValue;
	}

	private String classValue(String attributeName, String classValue) {
		Class<?> clazz = null;
		try {
			clazz = Classes.forName(classValue);
		} catch (NoSuchBeingException expected) {
			try {
				clazz = Classes.forName(defaultPackage + classValue);
			} catch (NoSuchBeingException ignored) {
			}
		}

		if (clazz == null) {
			if (defaultPackage == null) {
				throw new IllegalStateException(format("Bad binding definition. Class value |%s| for attribute <%s> seems using shorthand notation but there is no default package defined.", classValue, attributeName));
			}
			throw new IllegalStateException(format("Bad binding definition. Bad class value |%s| for attribute <%s> or/and bad default package |%s|.", classValue, attributeName, defaultPackage));
		}
		return clazz.getCanonicalName();
	}
}