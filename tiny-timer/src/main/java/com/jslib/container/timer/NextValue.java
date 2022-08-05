package com.jslib.container.timer;

import com.jslib.util.Strings;

/**
 * Next value from unit values returned by {@link #nextValue(CalendarEx.Unit, int)}. Beside the actual value this class
 * holds a flag about value overflowing. Value overflow occurs when next value is greater maximum allowed value in which
 * case returned value is the minimal one.
 * 
 * @author Iulian Rotaru
 */
class NextValue {
	private final int value;
	private final boolean overflow;

	public NextValue(int value, boolean overflow) {
		this.value = value;
		this.overflow = overflow;
	}

	public int getValue() {
		return value;
	}

	public boolean isOverflow() {
		return overflow;
	}

	@Override
	public String toString() {
		return Strings.toString(value, overflow);
	}
}
