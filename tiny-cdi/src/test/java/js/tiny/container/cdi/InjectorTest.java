package js.tiny.container.cdi;

import com.jslib.injector.IInjector;
import com.jslib.injector.Names;

public class InjectorTest {

	private IInjector injector;

	public void Given_When_Then() {

		injector.getInstance(String.class, Names.named("employee"));

		injector.getInstance(String.class, "employer");

	}
}
