package com.jslib.tiny.container.timer;

public class YearExpressionParser extends NumericExpressionParser {
	public YearExpressionParser() {
		super(CalendarUnit.YEAR);
	}

	@Override
	public NextValue getNextValue(int currentValue) {
		if (values == null) {
			throw new IllegalStateException("Not parsed expression.");
		}
		for (int value : values) {
			if (value >= currentValue) {
				return new NextValue(value, false);
			}
		}
		return null;
	}
}
