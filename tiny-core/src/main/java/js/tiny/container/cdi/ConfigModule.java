package js.tiny.container.cdi;

import static java.lang.String.format;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Singleton;
import js.converter.Converter;
import js.converter.ConverterRegistry;
import js.injector.AbstractModule;
import js.injector.IBindingBuilder;
import js.injector.ThreadScoped;
import js.lang.Config;
import js.util.Classes;

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

	private static final Map<String, Class<?>> SCOPE_CLASSES = new HashMap<>();
	static {
		SCOPE_CLASSES.put("Singleton", Singleton.class);
		SCOPE_CLASSES.put("ThreadScoped", ThreadScoped.class);
		SCOPE_CLASSES.put("SessionScoped", SessionScoped.class);
		SCOPE_CLASSES.put("RequestScoped", RequestScoped.class);
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
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}

	private String scopeValue(String scopeValue) {
		boolean simpleName = scopeValue.indexOf('.') == -1;
		if (simpleName) {
			Class<?> scopeClass = SCOPE_CLASSES.get(scopeValue);
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
			clazz = Class.forName(classValue);
		} catch (ClassNotFoundException e) {
			try {
				clazz = Class.forName(defaultPackage + classValue);
			} catch (ClassNotFoundException e1) {
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