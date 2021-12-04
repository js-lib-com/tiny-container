package js.tiny.container.cdi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import js.converter.Converter;
import js.converter.ConverterRegistry;
import js.injector.AbstractModule;
import js.injector.IBindingBuilder;
import js.lang.Config;

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

	private final Converter converter;
	private final Config config;
	private final String defaultPackage;

	public ConfigModule(Config config) {
		this.converter = ConverterRegistry.getConverter();
		this.config = config;
		this.defaultPackage = config.getAttribute("package", "") + ".";
	}

	@Override
	protected void configure() {
		config.getChildren().forEach(this::configure);
	}

	<T> void configure(Config bindingConfig) {
		@SuppressWarnings("unchecked")
		IBindingBuilder<T> bindingBuilder = bind(converter.asObject(classValue(bindingConfig.getAttribute("bind")), Class.class));
		bindingConfig.attributes((name, value) -> bindAttribute(bindingBuilder, name, value));
	}

	private String classValue(String value) {
		return value == null? null: value.indexOf('.') == -1 ? defaultPackage + value : value;
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
					value = classValue(value);
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
}