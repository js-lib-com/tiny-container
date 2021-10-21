package js.tiny.container.cdi;

import java.lang.annotation.Annotation;

import javax.inject.Scope;

public class InjectorTest {

	private IInjector injector;

	public void Given_When_Then() {


		injector.getInstance(String.class, Names.named("employee"));

		injector.getInstance(String.class, "employer");

		Scope scope = new Scope() {
			@Override
			public Class<? extends Annotation> annotationType() {
				return Scope.class;
			}
		};
	}
}
