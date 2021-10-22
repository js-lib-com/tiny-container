package js.tiny.container.cdi;

public class InjectorTest {

	private IInjector injector;

	public void Given_When_Then() {

		injector.getInstance(String.class, Names.named("employee"));

		injector.getInstance(String.class, "employer");

	}
}
