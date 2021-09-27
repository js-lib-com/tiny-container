package js.tiny.container.http;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import js.lang.SyntaxException;
import js.log.Log;
import js.log.LogFactory;
import js.util.Files;
import js.util.Params;
import js.util.Strings;

/**
 * HTTP content type instance and public constants. This class has a single constructor that parses a string value and
 * initializes internal immutable state. Provides getters for ContentType components: type, subtype and parameters. Also this
 * class provides predefined constant values for well known content types.
 * <p>
 * Here is described expected syntax for string value provided to constructor. If this syntax is not respected
 * {@link #ContentType(String)} throws syntax exception.
 * 
 * <pre>
 * content-type = mime-type *(OWS ";" OWS parameter)
 * mime-type = type "/" subtype
 * parameter = name "=" value
 * 
 * type = string
 * subtype = string
 * name = string
 * value = string "/" quoted-string
 * 
 * ; optional white space
 * OWS = *(SP / HTAB)
 * 
 * ; string contains any char less delimiters
 * ; quoted-string is string between double quotes
 * </pre>
 * <p>
 * This class provides factory methods for creating content type instances from string value or one suitable to represent a file
 * content. Content type to file mapping is based on file extension and cover only couple of, widely used, types.
 * <p>
 * Two ContentType instances are considered equal if have the the same {@link #mime} value, that is, parameters are not used for
 * equality test. Also hash code is based on the same mime field.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public class ContentType {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(ContentType.class);

	/** Default value for textual files. A textual file should be human-readable and must not contain binary data. */
	public static final ContentType TEXT_PLAIN = new ContentType("text/plain;charset=UTF-8");
	/** HyperText Markup Language (HTML) */
	public static final ContentType TEXT_HTML = new ContentType("text/html;charset=UTF-8");
	/** Extensible Markup Language (XML) */
	public static final ContentType TEXT_XML = new ContentType("text/xml;charset=UTF-8");
	/** Cascading Style Sheet (CSS) */
	public static final ContentType TEXT_CSS = new ContentType("text/css;charset=UTF-8");
	/** JavaScript */ 
	public static final ContentType APPLICATION_JAVASCRIPT = new ContentType("application/javascript;charset=UTF-8");
	/** Multipart MIME data streams for HTML forms that contain binary files. */
	public static final ContentType MULTIPART_FORM = new ContentType("multipart/form-data");
	/**
	 * Mixed is the primary subtype for multipart, and is intended for use when the body parts are independent and intended to
	 * be displayed serially.
	 */
	public static final ContentType MULTIPART_MIXED = new ContentType("multipart/mixed");
	/** HTTP form represented as list of name / value pairs of strings. */
	public static final ContentType URLENCODED_FORM = new ContentType("application/x-www-form-urlencoded");
	/**
	 * JSON format. Excerpt from https://www.iana.org/assignments/media-types/application/json: No "charset" parameter is
	 * defined for this registration. Adding one really has no effect on compliant recipients.
	 */
	public static final ContentType APPLICATION_JSON = new ContentType("application/json");
	/** Archive document (multiple files embedded). */
	public static final ContentType APPLICATION_STREAM = new ContentType("application/octet-stream");
	/** Portable Network Graphics */
	public static final ContentType IMAGE_PNG = new ContentType("image/png");
	/** JPEG images */
	public static final ContentType IMAGE_JPEG = new ContentType("image/jpeg");
	/** Graphics Interchange Format (GIF) */
	public static final ContentType IMAGE_GIF = new ContentType("image/gif");
	/** Tagged Image File Format (TIFF) */
	public static final ContentType IMAGE_TIFF = new ContentType("image/tiff");
	/** Scalable Vector Graphics (SVG) */
	public static final ContentType IMAGE_SVG = new ContentType("image/svg+xml");

	/** Content type for widespread file extensions. */
	public static final Map<String, ContentType> FILE_TYPES = new HashMap<>();
	static {
		FILE_TYPES.put("html", TEXT_HTML);
		FILE_TYPES.put("htm", TEXT_HTML);
		FILE_TYPES.put("xml", TEXT_XML);
		FILE_TYPES.put("css", TEXT_CSS);
		FILE_TYPES.put("js", APPLICATION_JAVASCRIPT);
		FILE_TYPES.put("json", APPLICATION_JSON);
		FILE_TYPES.put("png", IMAGE_PNG);
		FILE_TYPES.put("jpg", IMAGE_JPEG);
		FILE_TYPES.put("jpeg", IMAGE_JPEG);
		FILE_TYPES.put("gif", IMAGE_GIF);
		FILE_TYPES.put("tiff", IMAGE_TIFF);
		FILE_TYPES.put("svg", IMAGE_SVG);
	}

	/**
	 * Create a content type instance suitable to represent requested file. It is a trivial a approach using a map of common
	 * used file extension. Recognizes only couple, most used types; if extension is not recognized returns {@link #TEXT_HTML}.
	 * 
	 * @param file source file.
	 * @return content type discovered for requested file.
	 * @throws IllegalArgumentException if file parameter is null.
	 */
	public static ContentType forFile(File file) {
		Params.notNull(file, "File");
		ContentType contentType = FILE_TYPES.get(Files.getExtension(file));
		if (contentType == null) {
			log.debug("Unknown content type for |%s|. Replace with default |%s|.", file, TEXT_HTML);
			contentType = TEXT_HTML;
		}
		return contentType;
	}

	/**
	 * Parses content type value and returns newly created instance. Given value should obey syntax described by this class.
	 * This factory method just delegates {@link #ContentType(String)}. If <code>value</code> argument is null uses
	 * {@link #APPLICATION_JSON} as default.
	 * 
	 * @param value content type string value with syntax described by this class, possible null.
	 * @return content type instance for given value.
	 * @throws SyntaxException if <code>value</code> does not respect syntax described by this class.
	 */
	public static ContentType valueOf(String value) {
		if (value == null) {
			return ContentType.APPLICATION_JSON;
		}
		return new ContentType(value);
	}

	/** Content mime type. */
	private final String type;
	/** Content mime subtype. */
	private final String subtype;
	/** Cache this immutable instance mime value. */
	private final String mime;
	/** Optional content type parameters, usually charset. Null if missing. */
	private final Map<String, String> parameters;
	/** Full content type value, that is, mime type and parameters. */
	private final String value;

	/**
	 * Construct content type from string value. Value should be formatted as specified by HTTP Content-Type header, that is, it
	 * should have mime type, both type and subtype and optional parameters separated by mime type with semicolon.
	 * 
	 * @param value content type value.
	 * @throws IllegalArgumentException if <code>value</code> argument is null or empty.
	 * @throws SyntaxException if <code>value</code> does not respect syntax described by this class.
	 */
	public ContentType(String value) {
		Params.notNullOrEmpty(value, "Content type value");

		int parametersSeparatorIndex = value.indexOf(';');
		if (parametersSeparatorIndex != -1) {
			parameters = parseParameters(value.substring(parametersSeparatorIndex + 1).trim());
			value = value.substring(0, parametersSeparatorIndex).trim();
		} else {
			parameters = Collections.emptyMap();
		}

		int typesSeparatorIndex = value.indexOf('/');
		if (typesSeparatorIndex == -1) {
			throw new SyntaxException("Invalid content type value |%s|. Missing subtype separator.", value);
		}
		this.type = value.substring(0, typesSeparatorIndex).trim();
		this.subtype = value.substring(typesSeparatorIndex + 1).trim();
		this.mime = Strings.concat(type, '/', subtype);

		StringBuilder valueBuilder = new StringBuilder();
		valueBuilder.append(this.type);
		valueBuilder.append('/');
		valueBuilder.append(this.subtype);
		for (Map.Entry<String, String> parameter : parameters.entrySet()) {
			valueBuilder.append(';');
			valueBuilder.append(parameter.getKey());
			valueBuilder.append('=');
			valueBuilder.append(parameter.getValue());
		}
		this.value = valueBuilder.toString();
	}

	/**
	 * Get mime type for this content type instance.
	 * 
	 * @return mime type.
	 * @see #type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Get mime subtype for this content type instance.
	 * 
	 * @return mime subtype.
	 * @see #subtype
	 */
	public String getSubtype() {
		return subtype;
	}

	/**
	 * MIME value is just type and subtype, that is, content type without parameters.
	 * 
	 * @return mime value.
	 */
	public String getMIME() {
		return mime;
	}

	/**
	 * Test if this content type has the named parameter.
	 * 
	 * @param name parameter name.
	 * @return true if this content type has the names parameter.
	 * @throws IllegalArgumentException if <code>name</code> argument is null or empty.
	 */
	public boolean hasParameter(String name) {
		Params.notNullOrEmpty(name, "Parameter name");
		return parameters.containsKey(name);
	}

	/**
	 * Test if content type has a parameter with requested name and value.
	 * 
	 * @param name parameter name,
	 * @param value parameter value.
	 * @return true if content type has a parameter with requested name and value.
	 * @throws IllegalArgumentException if <code>name</code> argument is null or empty.
	 * @throws IllegalArgumentException if <code>value</code> argument is null or empty.
	 */
	public boolean hasParameter(String name, String value) {
		Params.notNullOrEmpty(name, "Parameter name");
		Params.notNullOrEmpty(value, "Parameter value");
		return value.equals(parameters.get(name));
	}

	/**
	 * Get parameter value or null if parameter does not exist.
	 * 
	 * @param name parameter name.
	 * @return parameter value, possible null.
	 * @throws IllegalArgumentException if <code>name</code> argument is null or empty.
	 */
	public String getParameter(String name) {
		Params.notNullOrEmpty(name, "Parameter name");
		return parameters.get(name);
	}

	/**
	 * Return full content type value, that is, type, subtype and parameters.
	 * 
	 * @return full content type value.
	 * @see #value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Test if this content type is of requested MIME type.
	 * 
	 * @param mime requested MIME type.
	 * @return true if this content type is of requested MIME type.
	 */
	public boolean isMIME(String mime) {
		return getMIME().equals(mime);
	}

	/**
	 * Test if this content type is HTML.
	 * 
	 * @return true if this content type is HTML.
	 */
	public boolean isHTML() {
		return TEXT_HTML.equals(this);
	}

	/**
	 * Test if this content type is XML.
	 * 
	 * @return true if this content type is XML.
	 */
	public boolean isXML() {
		return TEXT_XML.equals(this);
	}

	/**
	 * Test if this content type is JSON.
	 * 
	 * @return true if this content type is JSON.
	 */
	public boolean isJSON() {
		return APPLICATION_JSON.equals(this);
	}

	/**
	 * Test if this content type is multipart form.
	 * 
	 * @return true if this content type is multipart form.
	 */
	public boolean isMultipartForm() {
		return MULTIPART_FORM.equals(this);
	}

	/**
	 * Test if this content type is mixed multipart.
	 * 
	 * @return true if this content type is mixed multipart.
	 */
	public boolean isMultipartMixed() {
		return MULTIPART_MIXED.equals(this);
	}

	/**
	 * Test if this content type is URL encoded.
	 * 
	 * @return true if this content type is URL encoded.
	 */
	public boolean isUrlEncodedForm() {
		return URLENCODED_FORM.equals(this);
	}

	/**
	 * Test if this content type is bytes stream.
	 * 
	 * @return true if this content type is bytes stream.
	 */
	public boolean isByteStream() {
		return APPLICATION_STREAM.equals(this);
	}

	// --------------------------------------------------------------------------------------------
	// OBJECT

	/**
	 * Get content type instance string representation.
	 * 
	 * @return string representation.
	 */
	@Override
	public String toString() {
		return value;
	}

	/**
	 * Get hash code for this content type instance. This class hash code is based on {@link #mime} field.
	 * 
	 * @return hash code.
	 */
	@Override
	public int hashCode() {
		return mime.hashCode();
	}

	/**
	 * Predicates that test if given object equals this content type instance. Two content type instances are considered equal
	 * if they have the same {@link #mime} value.
	 * 
	 * @param obj another object to test for equality, possible null.
	 * @return true if given object is a content type with the same mime as this instance.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		return this.mime.equals(((ContentType) obj).mime);
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	/**
	 * Parse content type parameters. Given parameters expression should be valid accordingly grammar from class description; it
	 * should not start with parameters separator, that is, semicolon.
	 * 
	 * @param expression content type parameters expression.
	 * @return non empty parameters map.
	 * @throws SyntaxException if parameters expression is not valid.
	 */
	private static Map<String, String> parseParameters(String expression) {
		// charset = UTF-8

		Map<String, String> parameters = new HashMap<>();
		int parametersSeparatorIndex = 0;
		for (;;) {
			int valueSeparatorIndex = expression.indexOf('=', parametersSeparatorIndex);
			if (valueSeparatorIndex == -1) {
				break;
			}
			String name = expression.substring(parametersSeparatorIndex, valueSeparatorIndex).trim();

			++valueSeparatorIndex;
			parametersSeparatorIndex = expression.indexOf(';', valueSeparatorIndex);
			if (parametersSeparatorIndex == -1) {
				parametersSeparatorIndex = expression.length();
			}
			if (valueSeparatorIndex == parametersSeparatorIndex) {
				throw new SyntaxException("Invalid content type parameters |%s|. Value is empty.", expression);
			}

			if (parameters.put(name, expression.substring(valueSeparatorIndex, parametersSeparatorIndex).trim()) != null) {
				throw new SyntaxException("Invalid content type parameters |%s|. Name override |%s|.", expression, name);
			}
			++parametersSeparatorIndex;
		}

		if (parameters.isEmpty()) {
			throw new SyntaxException("Invalid content type parameters |%s|. Missing name/value separator.", expression);
		}

		return parameters;
	}
}
