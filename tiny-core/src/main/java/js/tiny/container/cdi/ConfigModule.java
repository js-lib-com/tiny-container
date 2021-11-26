package js.tiny.container.cdi;

import java.net.URI;

import javax.inject.Singleton;

import js.injector.AbstractModule;
import js.injector.IBindingBuilder;
import js.injector.SessionScoped;
import js.injector.ThreadScoped;
import js.lang.Config;

/**
 * Injector module initialized from managed classes collection. This specialized module traverses container managed classes,
 * creating injector bindings accordingly managed class instance type and scope.
 * 
 * @author Iulian Rotaru
 */
class ConfigModule extends AbstractModule {
	private final Config config;

	public ConfigModule(Config config) {
		this.config = config;
	}

	@Override
	protected void configure() {
		config.getChildren().forEach(this::configure);
	}

	private <T> void configure(Config classConfig) {
		ConfigBinding<T> binding = new ConfigBinding<>(classConfig);
		IBindingBuilder<T> bindingBuilder = bind(binding.interfaceClass);

		switch (binding.instanceType) {
		case LOCAL:
			bindingBuilder.to(binding.implementationClass);
			break;

		case REMOTE:
			bindingBuilder.on(binding.implementationURL);
			break;

		case SERVICE:
			bindingBuilder.service();
			break;

		default:
			throw new IllegalStateException("No provider for instance type " + binding.instanceType);
		}

		switch (binding.instanceScope) {
		case LOCAL:
			break;

		case APPLICATION:
			bindingBuilder.in(Singleton.class);
			break;

		case THREAD:
			bindingBuilder.in(ThreadScoped.class);
			break;

		case SESSION:
			bindingBuilder.in(SessionScoped.class);
			break;

		default:
			throw new IllegalStateException("No provider for instance scope " + binding.instanceScope);
		}
	}

	static class ConfigBinding<T> {
		final Class<T> interfaceClass;
		final Class<? extends T> implementationClass;
		final InstanceType instanceType;
		final InstanceScope instanceScope;
		final URI implementationURL;

		@SuppressWarnings("unchecked")
		public ConfigBinding(Config config) {
			this.implementationClass = config.getAttribute("class", Class.class);
			this.interfaceClass = config.getAttribute("interface", Class.class, this.implementationClass);
			this.instanceType = config.getAttribute("type", InstanceType.class, InstanceType.LOCAL);
			this.instanceScope = config.getAttribute("scope", InstanceScope.class, InstanceScope.LOCAL);
			this.implementationURL = config.getAttribute("url", URI.class);
		}
	}

	enum InstanceType {
		LOCAL, REMOTE, SERVICE
	}

	enum InstanceScope {
		LOCAL, APPLICATION, THREAD, SESSION
	}
}