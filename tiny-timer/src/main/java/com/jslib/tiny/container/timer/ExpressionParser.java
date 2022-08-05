package com.jslib.tiny.container.timer;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jslib.util.Params;

/**
 * Extensible parser for schedule expressions. Expression parser deals per se only with strict positive integer values; for
 * textual expressions it delegates external parsers - see {@link ExpressionParser#ExpressionParser(ITextualParser)}. Anyway,
 * this parser also compound expression, e.g. list, range and increment.
 * <p>
 * In order to process label based expressions this parser has a reference to an external textual parser. When this parser needs
 * to process single values it first delegates the textual parser and if {@link ITextualParser#parseText(String)} returns null
 * continue parsing the integer value - see {@link #parseValue(String)}.
 * 
 * @author Iulian Rotaru
 */
class ExpressionParser {
	private final SortedSet<Integer> values = new TreeSet<>();
	private final ITextualParser textualParser;

	public ExpressionParser() {
		this.textualParser = new ITextualParser() {
			@Override
			public Integer parseText(String text) {
				return null;
			}
		};
	}

	public ExpressionParser(ITextualParser textualParser) {
		this.textualParser = textualParser;
	}

	public SortedSet<Integer> parseExpression(String expression, int minimum, int maximum) {
		Params.notNullOrEmpty(expression, "Schedule expression");

		parseExpression(expression, minimum, maximum, value -> {
			if (value < minimum) {
				throw new IllegalArgumentException("Too small value: " + value);
			}
			if (value > maximum) {
				throw new IllegalArgumentException("Too large value: " + value);
			}
			values.add(value);
		});
		return values;
	}

	public void parseExpression(String expression, int minimum, int maximum, IValuesCollector values) {
		expression = expression.trim();

		// wildcard
		if (expression.equals("*")) {
			for (int i = minimum; i <= maximum; ++i) {
				values.add(i);
			}
			return;
		}

		// list
		if (expression.contains(",")) {
			String[] items = expression.split(",");
			if (items.length == 0) {
				throw new IllegalArgumentException("Empty list expression.");
			}
			for (String item : items) {
				parseRange(item.trim(), minimum, maximum, values);
			}
			return;
		}

		// increment
		int slashPosition = expression.indexOf('/');
		if (slashPosition > 0) {
			String startValue = expression.substring(0, slashPosition).trim();
			int start = startValue.equals("*") ? minimum : Integer.parseInt(startValue);
			int increment = Integer.parseInt(expression.substring(slashPosition + 1).trim());

			for (int value = start; value <= maximum; value += increment) {
				values.add(value);
			}
			return;
		}

		// range and single value
		parseRange(expression, minimum, maximum, values);
	}

	private static final Pattern RANGE_PATTERN;
	static {
		final String NUMBER = "\\s*-?\\d+\\s*";
		final String WORD = "\\s*(?:[1-5][a-z]{2})?\\s*[a-z]+\\s*[a-z]*\\s*";

		RANGE_PATTERN = Pattern.compile("(" + NUMBER + "|" + WORD + ")-(" + NUMBER + "|" + WORD + ")", Pattern.CASE_INSENSITIVE);
	}

	private void parseRange(String expression, int minimum, int maximum, IValuesCollector values) {
		Matcher matcher = RANGE_PATTERN.matcher(expression);
		if (!matcher.matches()) {
			values.add(parseValue(expression));
			return;
		}

		int start = parseValue(matcher.group(1).trim());
		int end = parseValue(matcher.group(2).trim());

		if (start < end) {
			for (int i = start; i <= end; ++i) {
				values.add(i);
			}
		} else if (start > end) {
			// add lower sub-range first
			for (int i = minimum; i <= end; ++i) {
				values.add(i);
			}
			for (int i = start; i <= maximum; ++i) {
				values.add(i);
			}
		} else {
			values.add(start);
		}
	}

	/**
	 * Parse single value expression. This method first delegates {@link ITextualParser#parseText(String)} and, if not null,
	 * returns its value. If textual parser does not recognize expression returns null and this method attempt to parse it as
	 * integer.
	 * 
	 * @param expression single value expression.
	 * @return numerical value represented by given expression.
	 * @throws IllegalArgumentException if expression is not accepted by textual parser and is not a valid integer.
	 */
	private Integer parseValue(String expression) {
		Integer value = textualParser.parseText(expression);
		if (value != null) {
			return value;
		}

		try {
			return Integer.parseInt(expression);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid numeric expression: " + expression);
		}
	}
}
