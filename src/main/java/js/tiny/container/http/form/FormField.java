package js.tiny.container.http.form;

import js.converter.Converter;
import js.converter.ConverterException;

/**
 * Simple name/value pair used to store form part value. This interface is used by {@link FormIterator} to facilitate access to
 * form field value.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public interface FormField extends Part {
	/**
	 * Get this field string value, possible empty.
	 * 
	 * @return this field value, possible empty but never null.
	 */
	String getValue();

	/**
	 * Get this field value promoted to requested value type. A value type is a primitive or a plain Java object that can be
	 * converted to a string - see {@link Converter}. It is caller responsibility to ensure that requested value type is
	 * compatible with this field string value.
	 * <p>
	 * If field string value is empty this method returns and empty value, specific to requested type, like false or zero but
	 * never null.
	 * 
	 * @param type desired value type.
	 * @param <T> generic value type.
	 * @return this field not null value.
	 * @throws ConverterException if string value conversion fails.
	 */
	<T> T getValue(Class<T> type);
}