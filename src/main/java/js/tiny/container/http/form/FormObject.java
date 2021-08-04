package js.tiny.container.http.form;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import js.converter.Converter;
import js.converter.ConverterException;
import js.converter.ConverterRegistry;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.http.ContentType;
import js.util.Classes;
import js.util.Files;
import js.util.Params;
import js.util.Strings;
import js.util.Types;

/**
 * Map form fields to plain Java object of not restricted graph complexity. In order to locate related object fields uses a
 * proprietary syntax for field names. It is named <code>Object Property Path</code> and allows to locate any field from object
 * graph. It is an hierarchical path like name, using dots for path component separators. For example <code>contact.name</code>
 * refers to name field of contact object from this form object. When used with arrays and lists, field name is the index, e.g.
 * <code>contacts.1.name</code> is the name of the second contact item.
 * <p>
 * Although FormObject class is public it is designed for framework internal usage; it helps injecting arbitrary Java objects on
 * resource or service methods. Container uses FormObject to create and initialize customer instance then inject
 * {@link #getValue()} as managed method argument. Note that form field names should obey <code>Object Property Path</code>
 * syntax and that managed method signature should have a single formal parameter.
 * 
 * <pre>
 * &#064;Override
 * public void saveCustomer(Customer customer) {
 * 	...
 * }
 * </pre>
 * <p>
 * Current implementation has couple limitations regarding aggregated types: supports only arrays and lists, both objects and
 * primitives. Primitives values are converted to component type using {@link Converter}. Arrays should be initialized with the
 * right dimension. Ignore form fields if array length is smaller that form values count.
 * <p>
 * This class implements best effort approach. If a property path does not denote an existing object field related form field is
 * ignored. Also, already mentioned, items exceeding array length are ignored.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public final class FormObject {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(FormObject.class);

	/** Factory for object and graph node instances. */
	private final NodeFactory factory;

	/** Object instance that need to be populated from form parts. */
	private Object object;

	/**
	 * Test constructor.
	 * 
	 * @param object object instance to populate.
	 */
	private FormObject(Object object) {
		this.factory = new NodeFactory();
		this.object = object;
	}

	/**
	 * Create form object instance of requested type and initialized it from HTTP request. HTTP request content type should be
	 * {@link ContentType#MULTIPART_FORM}. Field values are loaded in memory as object fields. If form contains streams they are
	 * saved on temporary files. Be aware that temporary files are removed at JVM exit.
	 * 
	 * @param httpRequest HTTP request carrying the multipart form,
	 * @param type object instance type.
	 * @throws IOException if HTTP request input stream read operation fails.
	 */
	public FormObject(HttpServletRequest httpRequest, Class<?> type) throws IOException {
		this(Classes.newInstance(type));

		FormIterator formIterator = new FormIteratorImpl(httpRequest);
		formIterator.forEach(new FormHandler() {
			@Override
			protected void stream(String name, String fileName, String contentType, InputStream inputStream) throws Throwable {
				setValue(name, new UploadedFileImpl(name, fileName, contentType, Files.copy(inputStream)));
			}

			@Override
			protected void field(String name, String value) throws Throwable {
				setValue(name, value);
			}
		});
	}

	/**
	 * Set value to object field identified by object property path. Form this class perspective an object is a graph of value
	 * types. A value type is a primitive value or a related boxing class. Also any class that can be converted to a primitive
	 * are included; for example {@link File} or {@link URL} are value types since can be converted to/from strings. Opposite to
	 * value types are compound entities, that is, objects, arrays and collections that aggregates value types or other compound
	 * entities.
	 * <p>
	 * To sum up, an object is a graph with compound entities and value types as nodes where value types are leafs. The
	 * <code>propertyPath</code> is the path through graph nodes till reach the value type and basically is a dot separated
	 * field names list.
	 * 
	 * @param propertyPath object property path,
	 * @param value value to set.
	 * @throws ConverterException if value is primitive and fail to convert to field type.
	 * @throws IllegalAccessException this exception is required by reflective field signature but never thrown.
	 */
	private void setValue(String propertyPath, Object value) throws ConverterException, IllegalAccessException {
		List<String> nodeIDs = Strings.split(propertyPath, '.');
		int lastNodeIndex = nodeIDs.size() - 1;
		Node node = new ObjectNode(object);
		for (int index = 0; index < lastNodeIndex; index++) {
			node = node.getChild(nodeIDs.get(index));
			if (node == null) {
				return;
			}
		}
		node.setValue(nodeIDs.get(lastNodeIndex), value);
	}

	/**
	 * Get initialized form object with type provided to constructor.
	 * 
	 * @return form object.
	 */
	public Object getValue() {
		return object;
	}

	// --------------------------------------------------------------------------------------------
	// INTERNAL CLASSES

	/**
	 * A node from object graph that can be a value type or a compound entity. A value type is a primitive or a class that can
	 * be converted to a primitive. A compound entity aggregates value types or other compound entities.
	 * <p>
	 * A node has a value setter - if value type and a collection of children identified by an ID. A child ID can be a field
	 * name or an array/list index.
	 * 
	 * @author Iulian Rotaru
	 * @version final
	 */
	private static interface Node {
		/**
		 * Set value to child node identified by given ID.
		 * 
		 * @param childID child ID is a field name or array/list index.
		 * @param value node value.
		 * @throws ConverterException if value is primitive and fail to convert to field type.
		 * @throws IllegalAccessException this exception is required by reflective field signature but never thrown.
		 */
		void setValue(String childID, Object value) throws ConverterException, IllegalAccessException;

		/**
		 * Returns child node identified by its ID or null if not found. A child ID is the field name or array/list index.
		 * 
		 * @param childID child ID is a field name or array/list index.
		 * @return child node or null.
		 * @throws IllegalAccessException this exception is required by reflective field signature but never thrown.
		 */
		Node getChild(String childID) throws IllegalAccessException;
	}

	/**
	 * Factory for object and graph node instances.
	 * 
	 * @author Iulian Rotaru
	 * @version final
	 */
	private class NodeFactory {
		/**
		 * Create an object instance of the requested type. If type is a {@link List} uses
		 * {@link Classes#getListDefaultImplementation(Type)}.
		 * 
		 * @param type object type.
		 * @return newly created instance.
		 */
		public Object newInstance(Class<?> type) {
			if (Types.isKindOf(type, List.class)) {
				type = Classes.getListDefaultImplementation(type);
			}
			return Classes.newInstance(type);
		}

		/**
		 * Create form object node wrapping given plain Java object.
		 * 
		 * @param object plain Java object that created form object node should wrap,
		 * @param type component type used only if wrapped object is a list.
		 * @return node for requested class.
		 */
		public Node createNode(Object object, Class<?> type) {
			if (Types.isKindOf(object.getClass(), List.class)) {
				return new ListNode(object, type);
			}
			if (object.getClass().isArray()) {
				return new ArrayNode(object);
			}
			return new ObjectNode(object);
		}
	}

	/**
	 * Object graph node of generic object type.
	 * 
	 * @author Iulian Rotaru
	 * @version final
	 */
	private class ObjectNode implements Node {
		/** Field string value converter to object field type. */
		private final Converter converter;

		/** Node wrapped object. */
		private final Object object;

		/**
		 * Construct node object.
		 * 
		 * @param object wrapped object.
		 */
		public ObjectNode(Object object) {
			this.converter = ConverterRegistry.getConverter();
			this.object = object;
		}

		/**
		 * Get child node by name creating it on the fly if not set. This method attempt to retrieve child node for wrapped
		 * object field with requested name; it returns null if there is no object field with requested child name. If field
		 * value is null create child node and set it.
		 * 
		 * @param childName child name.
		 * @return child node, newly created or retrieved from wrapped object field.
		 */
		@Override
		public Node getChild(String childName) throws IllegalAccessException {
			Field field = null;
			try {
				field = Classes.getFieldEx(object.getClass(), Strings.toMemberName(childName));
			} catch (NoSuchFieldException e) {
				// best effort approach: form to object mapping is not so strictly but still log missing fields
				log.debug("Missing field |%s| from object |%s|.", childName, object.getClass());
				return null;
			}

			Object child = field.get(object);
			// not test coverable condition: if child is null child instanceof Collection cannot be true
			if (child == null || (child instanceof Collection<?> && ((Collection<?>) child).size() == 0)) {
				child = FormObject.this.factory.newInstance(field.getType());
				field.set(object, child);
			}
			return FormObject.this.factory.createNode(child, type(field));
		}

		/**
		 * Set value to wrapped object field identified by requested child name. Given string value is converted to wrapped
		 * object type using {@link Converter}.
		 *
		 * @param childName wrapped object field name,
		 * @param value string value loaded from form field, null and empty accepted.
		 * @throws ConverterException if field value conversion fails.
		 * @throws IllegalAccessException this exception is required by reflective field signature but never thrown.
		 */
		@Override
		public void setValue(String childName, Object value) throws ConverterException, IllegalAccessException {
			Field field = null;
			try {
				field = Classes.getFieldEx(object.getClass(), Strings.toMemberName(childName));
			} catch (NoSuchFieldException e) {
				// best effort approach: form to object mapping is not so strictly - i.e. ignores missing fields
				log.debug("Missing field |%s| from object |%s|.", childName, object.getClass());
				return;
			}

			if (value instanceof UploadedFile) {
				if (Types.isKindOf(field.getType(), File.class)) {
					field.set(object, ((UploadedFile) value).getFile());
				} else {
					field.set(object, value);
				}
			} else {
				field.set(object, converter.asObject((String) value, field.getType()));
			}
		}
	}

	/**
	 * Object graph node of list type.
	 * 
	 * @author Iulian Rotaru
	 * @version final
	 */
	@SuppressWarnings("unchecked")
	private class ListNode implements Node {
		/** Field string value converter to list component type. */
		private final Converter converter;

		/** Node wrapped list. */
		private final List<Object> list;

		/** List component type. */
		private final Class<?> componentType;

		/**
		 * Construct list node for a given list component type.
		 * 
		 * @param object wrapped list,
		 * @param componentType list component type.
		 */
		public ListNode(Object object, Class<?> componentType) {
			this.converter = ConverterRegistry.getConverter();
			this.list = (List<Object>) object;
			this.componentType = componentType;
		}

		/**
		 * Get child node at specified index. Child node type depends on {@link #componentType}.
		 * 
		 * @param childIndex child index.
		 * @return child node at requested index.
		 * @throws IllegalArgumentException if child index argument is not a numeric value.
		 */
		@Override
		public Node getChild(String childIndex) throws IllegalArgumentException {
			Params.isNumeric(childIndex, "Child index");
			int index = Integer.parseInt(childIndex);
			ensureCapacity(index, componentType);
			return FormObject.this.factory.createNode(list.get(index), componentType);
		}

		/**
		 * Set value to child node identified by given child index. Given string value is converted to list component type, see
		 * {@link #componentType} using {@link Converter}.
		 * 
		 * @param childIndex child index,
		 * @param value string value, null and empty accepted, loaded from field.
		 * @throws IllegalArgumentException if child index argument is not a numeric value.
		 * @throws ConverterException if field value conversion fails.
		 */
		@Override
		public void setValue(String childIndex, Object value) throws IllegalArgumentException, ConverterException {
			Params.isNumeric(childIndex, "Child index");
			int index = Integer.parseInt(childIndex);
			ensureCapacity(index);
			if (!String.class.equals(componentType)) {
				value = converter.asObject((String) value, componentType);
			}
			list.set(index, value);
		}

		/**
		 * Add null value(s) to wrapped list till given index, ensuring list capacity.
		 * 
		 * @param index list index till where to add null value(s).
		 */
		private void ensureCapacity(int index) {
			int overflow = index - list.size();
			while (overflow-- >= 0) {
				list.add(null);
			}
		}

		/**
		 * Add item(s) of requested type to wrapped list till index position, ensuring list capacity.
		 * 
		 * @param index list index till where item(s) should be added,
		 * @param type the type of item instance to create.
		 */
		private void ensureCapacity(int index, Class<?> type) {
			int overflow = index - list.size();
			while (overflow-- >= 0) {
				list.add(FormObject.this.factory.newInstance(type));
			}
		}
	}

	/**
	 * Object graph node of array type. This node is specialized on array fields. Note that array field should be initialized
	 * with dimension known on form development. If want to avoid dependency on form design uses list type, see {@link ListNode}
	 * .
	 * 
	 * @author Iulian Rotaru
	 * @version final
	 */
	private class ArrayNode implements Node {
		/** Field string value converter to array component type. */
		private final Converter converter;

		/** Node wrapped array. */
		private final Object array;

		/** Array component type. */
		private final Class<?> componentType;

		/**
		 * Construct array node.
		 * 
		 * @param array wrapped array.
		 */
		public ArrayNode(Object array) {
			this.converter = ConverterRegistry.getConverter();
			this.array = array;
			this.componentType = array.getClass().getComponentType();
		}

		/**
		 * Get child node at specified index, creating it on the fly if missing. Child node type depends on
		 * {@link #componentType}.
		 * 
		 * @param childIndex child index.
		 * @return child node at requested index, freshly created or reused from array.
		 * @throws IllegalArgumentException if child index argument is not a numeric value.
		 */
		@Override
		public Node getChild(String childIndex) throws IllegalArgumentException {
			Params.isNumeric(childIndex, "Child index");
			int index = Integer.parseInt(childIndex);
			Object item = Array.get(array, index);
			if (item == null) {
				Array.set(array, index, item = FormObject.this.factory.newInstance(componentType));
			}
			return FormObject.this.factory.createNode(item, componentType);
		}

		/**
		 * Set value to child specified by given index. If child index exceeds array length this setter does nothing and value
		 * is ignored.
		 * <p>
		 * Given string value is converted to array type, see {@link #componentType} using {@link Converter}.
		 * 
		 * @param childIndex node child index,
		 * @param value string value, null and empty accepted, loaded from field.
		 * @throws IllegalArgumentException if child index argument is not a numeric value.
		 * @throws ConverterException if field value conversion fails.
		 */
		@Override
		public void setValue(String childIndex, Object value) throws IllegalArgumentException, ConverterException {
			Params.isNumeric(childIndex, "Child index");
			int index = Integer.parseInt(childIndex);
			if (index >= Array.getLength(array)) {
				log.debug("Array capacity exceeded. Ingore value |%s| for array index |%d|.", value, index);
				return;
			}
			if (!String.class.equals(componentType)) {
				value = converter.asObject((String) value, componentType);
			}
			Array.set(array, index, value);
		}
	}

	/**
	 * Return field class or actual type argument if given field is a list.
	 * 
	 * @param field Java reflective class field.
	 * @return field type.
	 */
	private static Class<?> type(Field field) {
		if (Types.isKindOf(field.getType(), List.class)) {
			// for the purpose of this implementation only first parameterized
			// type matters, as result from list declaration List<E>
			return (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
		}
		return field.getType();
	}
}
