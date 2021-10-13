package js.tiny.container.service;

import java.util.List;

import js.converter.Converter;
import js.converter.ConverterException;
import js.lang.BugError;
import js.lang.Config;
import js.tiny.container.InstanceType;
import js.tiny.container.spi.IInstancePostConstructionProcessor;
import js.tiny.container.spi.IManagedClass;
import js.util.Classes;

/**
 * Initialize instance fields from managed class configuration object. Instance initializer reads name / value pairs from
 * <code>instance-field</code> configuration element, see sample below. String value is converted to instance field type using
 * {@link Converter#asObject(String, Class)} utility. This means that configured value should be convertible to field type,
 * otherwise {@link ConverterException} is thrown.
 * <p>
 * In sample there is a <code>person</code> managed instance that has a configuration section. Configuration declares three
 * instance fields that will be initialized with defined values every time a new js.app.Person instance is created.
 * 
 * <pre>
 * &lt;managed-classes&gt;
 * 	&lt;person class="js.app.Person" /&gt;
 * &lt;/managed-classes&gt;
 * ...
 * &lt;person&gt;
 * 	&lt;instance-field name='name' value='John Doe' /&gt;
 * 	&lt;instance-field name='age' value='54' /&gt;
 * 	&lt;instance-field name='married' value='false' /&gt;
 * &lt;/person&gt;
 * ...
 * class Person {
 * 	private String name;
 * 	private int age;
 * 	private boolean married;
 * }
 * </pre>
 * 
 * Instance initialization can be performed only on {@link InstanceType#POJO} types. Not obeying this constrain is considered a
 * bug and {@link BugError} is thrown. Finally, if managed class has no configuration object this processor does nothing, that
 * is, returns given instance as it is.
 * 
 * @author Iulian Rotaru
 */
public class InstanceFieldsInitializationProcessor implements IInstancePostConstructionProcessor {
	@Override
	public Priority getPriority() {
		return Priority.INJECT;
	}

	/**
	 * Initialize instance fields from managed class configuration object.
	 * 
	 * @param managedClass managed class,
	 * @param instance instance of managed class.
	 * @throws ConverterException if configured value cannot be converted to field type.
	 * @throws BugError if attempt to assign instance field to not POJO type.
	 */
	@Override
	public void onInstancePostConstruction(IManagedClass managedClass, Object instance) {
		Config config = managedClass.getConfig();
		if (config == null) {
			return;
		}
		List<Config> fields = config.findChildren("instance-field");
		if (!fields.isEmpty() && !InstanceType.POJO.equals(managedClass.getInstanceType())) {
			throw new BugError("Cannot assign instance field on non %s type.", InstanceType.POJO);
		}
		for (Config field : fields) {
			Classes.setFieldValue(instance, field.getAttribute("name"), field.getAttribute("value"));
		}
	}
}
