package js.tiny.container.spi;

public enum Priority {
	SECURITY(100), PERFMON(200), FIRST(300), HIGH(400), NORMAL(500), LOW(600), LAST(700);

	private int base;

	private Priority(int base) {
		this.base = base;
	}

	public int value(int order) {
		return base + order;
	}
}