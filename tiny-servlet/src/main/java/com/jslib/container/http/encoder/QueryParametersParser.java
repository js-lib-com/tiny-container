package com.jslib.container.http.encoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;

import com.jslib.converter.Converter;
import com.jslib.converter.ConverterException;
import com.jslib.converter.ConverterRegistry;
import com.jslib.lang.BugError;
import com.jslib.lang.SyntaxException;
import com.jslib.util.Classes;
import com.jslib.util.Strings;
import com.jslib.util.Types;

/**
 * Parse parameters encoded into URL query string format. This parser can be used for both URL query string from HTTP request
 * and form content encoded <code>application/x-www-form-urlencoded</code>. There are specialized constructors for both cases:
 * {@link QueryParametersParser#QueryParametersParser(String)}, respective
 * {@link QueryParametersParser#QueryParametersParser(InputStream)}. The actual parser is implemented by {@link #parse(Reader)}
 * that returns a list of decoded parameters. Constructors merely store parsed parameters into {@link #parameters} field.
 * <p>
 * After construction internal parameters list is initialized. Next step is to get method invocation arguments from decoded
 * parameters.
 * 
 * <pre>
 * QueryParameters queryParameters = new QueryParameters(httpRequest);
 * Type[] formalParameters = new Type[] { String.class, String.class, String.class };
 * Object[] arguments = queryParameters.getArguments(formalParameters);
 * </pre>
 * 
 * Method {@link #getArguments(Type[])} takes formal parameters types and returns invocation arguments mapped by position. Note
 * that mapping by name is not possible since Java does not preserve parameter names on run-time. This forces caller to ensure
 * given formal parameters are in the same order as decoded parameters. This class uses {@link #asObject(String, Type)} helper
 * method to actually create argument values of proper type, accordingly requested formal parameter types.
 * <p>
 * It is legal for a parameter value to be missing so next construct is acceptable:
 * <code>name1=value&amp;name2=&amp;name3=value3</code> . Anyway, even if value is missing, name/value separator (=) should be
 * present. When value is missing parameter is initialized with an empty value accordingly formal parameter type, see
 * {@link Types#getEmptyValue(Type)}.
 * <p>
 * This parser supports an extension to query string syntax. If there is a single parameter it is legal to have only value,
 * without name and separator. This value is said to be a <code>raw value</code>. For example, instead of using URL like
 * <code>.../captha/image?token=1234</code> is possbile to use <code>.../captha/image?1234</code>.
 * <p>
 * Query parameters implementation is thread safe; it has all fields final and parser is implemented functional.
 * 
 * @author Iulian Rotaru
 * @version final
 */
final class QueryParametersParser {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(QueryParametersParser.class);

	/** Reusable empty reader for null query string. */
	private static final Reader EMPTY_READER = new Reader() {
		@Override
		public int read(char[] cbuf, int off, int len) throws IOException {
			return -1;
		}

		@Override
		public void close() throws IOException {
			throw new UnsupportedOperationException();
		}
	};

	/** Parsed parameters list. */
	private final List<Parameter> parameters;

	/**
	 * Construct query parameters for URL query string from HTTP request. This constructor creates a reader for given query
	 * string and delegates the {@link #parse(Reader)}. Trailing question mark (?) is ignored. If query string is null resulting
	 * parameters list is empty.
	 * 
	 * @param queryString source query string, null accepted.
	 * @throws IOException for the very unlikely fail to read request query string.
	 * @throws UnsupportedEncodingException if request query string encoding is not UTF-8.
	 * @throws SyntaxException if query string format is invalid.
	 */
	public QueryParametersParser(String queryString) throws IOException {
		Reader reader = null;

		if (queryString == null) {
			reader = EMPTY_READER;
		} else {
			// from API is not very clear if query string has leading separator so decide to play safe
			if (queryString.charAt(0) == '?') {
				queryString = queryString.substring(1);
			}
			reader = new StringReader(queryString);
		}

		this.parameters = parse(reader);
	}

	/**
	 * Construct query parameters for input stream, usually form content encoded <code>application/x-www-form-urlencoded</code>.
	 * This constructor just convert input stream to reader and delegates the {@link #parse(Reader)}.
	 * 
	 * @param inputStream source input stream carrying URL encoded parameters.
	 * @throws IOException if stream read operations fail.
	 * @throws UnsupportedEncodingException if URL encoded parameters encoding is not UTF-8.
	 * @throws SyntaxException if URL encoded parameters are not properly encoded.
	 */
	public QueryParametersParser(InputStream inputStream) throws IOException {
		this.parameters = parse(new InputStreamReader(inputStream, "UTF-8"));
	}

	/**
	 * Parse URL query parameters provided by source reader. Detect query parameters name and and value and update a list of
	 * parsed parameters. Takes care to decode escaped characters from both parameter names and values.
	 * <p>
	 * This parser expect UTF-8 for text encoding and throws unsupported encoding for different codes.
	 * 
	 * @param reader source reader.
	 * @return list of parsed parameters.
	 * @throws IOException if read operation fails.
	 * @throws UnsupportedEncodingException if URL encoded parameters encoding is not UTF-8.
	 * @throws SyntaxException if URL query parameters are not properly encoded.
	 */
	private static List<Parameter> parse(Reader reader) throws IOException {
		List<Parameter> parameters = new ArrayList<Parameter>();
		Parameter parameter = new Parameter();

		State state = State.NAME;
		for (;;) {
			int b = reader.read();
			if (b == -1) {
				// conclude last parameter value
				if (parameter.isEmpty()) {
					break;
				}
				if (parameters.isEmpty() && parameter.isRawValue()) {
					parameter.commitRawValue();
				} else {
					parameter.commitValue();
				}
				parameters.add(parameter);
				break;
			}
			char c = (char) b;

			switch (state) {
			case NAME:
				switch (c) {
				case '=':
					state = State.VALUE;
					parameter.commitName();
					break;

				case '&':
					if (parameter.getBuilder().isEmpty()) {
						throw new SyntaxException("Invalid query string. Empty parameter name.");
					} else {
						throw new SyntaxException("Invalid query string parameter |%s|. Missing name/value separator.", parameter.getBuilder());
					}

				default:
					parameter.append(c);
				}
				break;

			case VALUE:
				switch (c) {
				case '&':
					state = State.NAME;
					parameter.commitValue();
					parameters.add(parameter);
					parameter = new Parameter();
					break;

				case '=':
					throw new SyntaxException("Invalid query string parameter |%s|. Unescaped '=' character.", parameter.getBuilder());

				default:
					parameter.append(c);
				}
			}
		}

		return parameters;
	}

	/**
	 * Get an array of arguments suitable for a method invocation. This method did its best to fill the arguments array. The
	 * logic is simple: ignore query extra parameters and set to null the missing ones. Both cases are recorded to debug log.
	 * Returned arguments array has the same size as requested formal parameters array.
	 * <p>
	 * Parameter values from parsed {@link #parameters} are converted to requested type using {@link #asObject(String, Type)}
	 * utility method. If formal parameters has a single type that has no converter uses reflection to map query parameters to
	 * object field by name.
	 * <p>
	 * Note that because parameter names are not preserved on run-time, query parameters are mapped by position to invocation
	 * arguments. This means is caller responsibility to match formal parameters order and type with actual query string
	 * parameters.
	 * 
	 * @param formalParameters formal parameters list.
	 * @return invocation arguments array.
	 * @throws ConverterException if string value cannot be converted to requested type.
	 * @throws BugError if type is generic and is not an array like.
	 */
	public Object[] getArguments(Type[] formalParameters) {
		if (formalParameters.length == 0) {
			return new Object[0];
		}
		Object[] arguments = new Object[formalParameters.length];

		if (isObject(formalParameters)) {
			// if there is single formal parameter and it is not a value type create object instance and initialize fields from
			// request parameters; object class should have no arguments constructor
			Class<?> type = (Class<?>) formalParameters[0];
			Object object = Classes.newInstance(type);
			for (Parameter parameter : parameters) {
				Field field = Classes.getField(type, Strings.toMemberName(parameter.getName()));
				Classes.setFieldValue(object, field, asObject(parameter.getValue(), field.getType()));
			}
			return new Object[] { object };
		}

		if (isMap(formalParameters)) {
			Map<String, String> map = Classes.newMap(formalParameters[0]);
			for (Parameter parameter : parameters) {
				map.put(parameter.getName(), parameter.getValue());
			}
			return new Object[] { map };
		}

		int i = 0, argumentsCount = Math.min(formalParameters.length, parameters.size());
		for (i = 0; i < argumentsCount; ++i) {
			arguments[i] = asObject(parameters.get(i).getValue(), formalParameters[i]);
		}
		for (; i < formalParameters.length; ++i) {
			log.debug("Missing request parameter |%s|. Set it to null.", i, formalParameters[i]);
			arguments[i] = null;
		}
		for (; i < parameters.size(); ++i) {
			log.debug("Unused request parameter |%s|. Ignore it.", parameters.get(i));
		}
		return arguments;
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	/**
	 * Helper method to convert string parameter to an instance of a given type. Requested <code>type</code> should be a value
	 * type as accepted by converter package or an array / collection of value types. In the later case <code>value</code>
	 * should be a comma separated string of items. If given <code>value</code> is null returns an empty value as defined by
	 * {@link Types#getEmptyValue(Type)}.
	 * 
	 * @param value parameter value, possible null,
	 * @param type parameter type.
	 * @param <T> instance type.
	 * @return newly created parameter type instance.
	 * @throws ConverterException if string value cannot be converted to requested type.
	 * @throws BugError if type is generic and is not an array like.
	 */
	@SuppressWarnings("unchecked")
	private static <T> T asObject(String value, Type type) {
		if (value == null) {
			return (T) Types.getEmptyValue(type);
		}

		if (!Types.isArrayLike(type)) {
			if (!(type instanceof Class)) {
				throw new BugError("Generic value types are not supported.");
			}
			if (ConverterRegistry.hasType(type)) {
				return ConverterRegistry.getConverter().asObject(value, (Class<T>) type);
			}
			log.debug("Missing converter for query parameter of type |%s|.", type);
			return (T) Types.getEmptyValue(type);
		}

		// here we have an array/collection represented as comma separated primitives
		List<String> strings = Strings.split(value, ',');
		if (type == String[].class) {
			return (T) strings.toArray(new String[strings.size()]);
		}
		if (Types.isKindOf(type, Collection.class)) {
			Type collectionType = type;
			Class<?> itemType = String.class;
			if (type instanceof ParameterizedType) {
				collectionType = ((ParameterizedType) type).getRawType();
				itemType = (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
			}
			Collection<Object> collection = Classes.newCollection(collectionType);
			Converter converter = ConverterRegistry.getConverter();
			for (String s : strings) {
				collection.add(converter.asObject(s.trim(), itemType));
			}
			return (T) collection;
		}

		throw new BugError("Type not supported |%s|.", type);
	}

	/**
	 * Test if method formal parameters designates a strict object, that is, is not primitive, array, collection or map.
	 * 
	 * @param formalParameters method formal parameters list.
	 * @return true if formal parameters designates a strict object.
	 */
	private static boolean isObject(Type[] formalParameters) {
		if (formalParameters.length != 1) {
			return false;
		}
		final Type type = formalParameters[0];
		if (!(type instanceof Class)) {
			return false;
		}
		if (Types.isPrimitive(type)) {
			return false;
		}
		if (Types.isArrayLike(type)) {
			return false;
		}
		if (Types.isMap(type)) {
			return false;
		}
		if (ConverterRegistry.hasType(type)) {
			return false;
		}
		return true;
	}

	private static boolean isMap(Type[] formalParameters) {
		if (formalParameters.length != 1) {
			return false;
		}
		final Type type = formalParameters[0];
		return Types.isMap(type);
	}

	// --------------------------------------------------------------------------------------------
	// INNER CLASSES

	/**
	 * Query parameter wraps a single parameter from query string. It has a name and a value. This class is specifically
	 * designed for integration with query string parser. It has an internal string builder, used to collect characters from
	 * stream, for both parameter name and value, see {@link #append(char)}. Also has methods to control building life cycle:
	 * {@link #commitName()} marks name processing end and {@link #commitValue()} conclude entire parameter parsing.
	 * <p>
	 * Because parameter instance has internal state that control string builder usage, it is not reusable. Create a new
	 * instance for every parameter from query string.
	 * 
	 * @author Iulian Rotaru
	 * @version final
	 */
	private static class Parameter {
		/** String builder used for both parameter name and value. */
		private final StringBuilder builder = new StringBuilder();
		/** Parameter name. */
		private String name;
		/** Parameter value. */
		private String value;

		/**
		 * Append character to string builder.
		 * 
		 * @param c character to add.
		 */
		public void append(char c) {
			builder.append(c);
		}

		/**
		 * Copy string builder content to parameter name and reset the builder.
		 * 
		 * @throws SyntaxException if parameter name is empty.
		 * @throws UnsupportedEncodingException if name is not properly encoded UTF-8.
		 */
		public void commitName() throws UnsupportedEncodingException {
			if (builder.length() == 0) {
				throw new SyntaxException("Invalid query string. Empty parameter name.");
			}
			name = URLDecoder.decode(builder.toString(), "UTF-8");
			builder.setLength(0);
		}

		/**
		 * Decode string builder content and copy result to parameter value.
		 * 
		 * @return true if parameter value was decoded.
		 * @throws SyntaxException if parameter name is null, most probably because of missing '=' separator.
		 * @throws UnsupportedEncodingException if value is not properly encoded UTF-8.
		 */
		public boolean commitValue() throws UnsupportedEncodingException {
			// name is null only if commit name was not invoked
			// if builder has some data but name is null then '=' separator was not found
			// this condition can occur only if query string ends before separator found

			// do not test for empty name since is already tested on commit name

			if (builder.length() > 0 && name == null) {
				throw new SyntaxException("Invalid query string parameter |%s|. Missing name/value separator.", builder.toString());
			}
			if (builder.length() == 0) {
				return false;
			}
			value = URLDecoder.decode(builder.toString(), "UTF-8");
			return true;
		}

		/**
		 * Commit raw value does actually copy internal builder content to this parameter value. Name is not used by raw values
		 * and is set to a neutral value.
		 * <p>
		 * A raw value is a query parameter that does not respect query string syntax, that is, is a continuous string without
		 * name/value separator. Raw value is used as a simplified, not standard, alternative when there is a single parameter.
		 * For example, instead of using URL like <code>.../captha/image?token=1234</code> is possbile to use
		 * <code>.../captha/image?1234</code>.
		 * 
		 * @throws UnsupportedEncodingException if value is not properly encoded UTF-8.
		 */
		public void commitRawValue() throws UnsupportedEncodingException {
			name = "null";
			value = URLDecoder.decode(builder.toString(), "UTF-8");
		}

		/**
		 * Test if this parameter is empty.
		 * 
		 * @return true if this parameter is empty.
		 */
		public boolean isEmpty() {
			return builder.length() == 0;
		}

		/**
		 * Test if this parameter is a raw value. A raw value has no name/value separator so {@link #commitName()} is not
		 * executed when this predicate is tested.
		 * 
		 * @return true if this parameter is a raw value.
		 */
		public boolean isRawValue() {
			return name == null;
		}

		/**
		 * Get parameter name.
		 * 
		 * @return parameter name;
		 */
		public String getName() {
			return name;
		}

		/**
		 * Get parameter value.
		 * 
		 * @return parameter value.
		 */
		public String getValue() {
			return value;
		}

		/**
		 * Get string builder value, for debugging purposes.
		 * 
		 * @return builder content.
		 */
		public String getBuilder() {
			return builder.toString();
		}
	}

	/**
	 * States for query parameters parser.
	 * 
	 * @author Iulian Rotaru
	 * @version final
	 */
	private static enum State {
		/** Processing parameter name. */
		NAME,
		/** Processing parameter value. */
		VALUE
	}
}
