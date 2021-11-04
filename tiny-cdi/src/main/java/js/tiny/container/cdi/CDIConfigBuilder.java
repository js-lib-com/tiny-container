package js.tiny.container.cdi;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

import com.jslib.injector.IModule;
import com.jslib.injector.IProvider;
import com.jslib.injector.ScopedProvider;

import js.lang.Config;
import js.lang.ConfigBuilder;
import js.lang.ConfigException;
import js.log.Log;
import js.log.LogFactory;

public class CDIConfigBuilder extends ConfigBuilder {
	private static final Log log = LogFactory.getLog(CDIConfigBuilder.class);

	private final List<Config> managedClasses = new ArrayList<>();

	public void addModule(IModule module) {
		module.bindings().forEach(binding -> {
			Provider<?> provider = binding.provider();
			log.debug(provider);
			
			if(provider instanceof ScopedProvider) {
				//((ScopedProvider)provider).getScopeAnnotation()
			}
			
			if (!(provider instanceof IProvider)) {
				return;
			}

			IProvider<?> typedProvider = (IProvider<?>) provider;

			Config config = new Config("managed-class");
			config.setAttribute("interface", binding.key().type().getCanonicalName());
			config.setAttribute("class", typedProvider.type().getCanonicalName());
			
			
			managedClasses.add(config);
		});
	}

	@Override
	public Config build() throws ConfigException {
		Config config = new Config("app");

		Config section = new Config("managed-classes");
		config.addChild(section);
		managedClasses.forEach(managedClass -> {
			section.addChild(managedClass);
		});

		return config;
	}

	public List<Config> getManagedClasses() {
		return managedClasses;
	}
}
