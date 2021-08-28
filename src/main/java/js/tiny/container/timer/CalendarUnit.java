package js.tiny.container.timer;

import java.util.Calendar;

enum CalendarUnit {
	YEAR(Calendar.YEAR), MONTH(Calendar.MONTH), DAY(Calendar.DAY_OF_MONTH), HOUR(Calendar.HOUR_OF_DAY), MINUTE(Calendar.MINUTE), SECOND(Calendar.SECOND);

	int value;

	private CalendarUnit(int value) {
		this.value = value;
	}

	public static int length() {
		return values().length;
	}

	public static CalendarUnit get(int index) {
		return values()[index];
	}

	public CalendarUnit getParentUnit() {
		return values()[this.ordinal() - 1];
	}
}