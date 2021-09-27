package js.tiny.container.timer;

import java.util.SortedSet;

abstract class BaseExpressionParser implements IScheduleExpressionParser {
	protected SortedSet<Integer> values;

	@Override
	public int getMinimumValue() {
		if (values == null) {
			throw new IllegalStateException("Not parsed expression.");
		}
		return values.first();
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
		return new NextValue(values.first(), true);
	}
}
