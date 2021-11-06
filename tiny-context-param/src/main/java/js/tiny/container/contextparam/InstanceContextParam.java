package js.tiny.container.contextparam;

import java.lang.reflect.Modifier;

import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerServiceProvider;
import js.tiny.container.spi.IInstancePostConstructProcessor;
import js.tiny.container.spi.IManagedClass;

public class InstanceContextParam extends BaseContextParam implements IInstancePostConstructProcessor {
	public InstanceContextParam(IContainer container) {
		super(container);
	}

	@Override
	public Priority getPriority() {
		return Priority.INJECT;
	}

	@Override
	public <T> void onInstancePostConstruct(IManagedClass<T> managedClass, T instance) {
		processFields(managedClass, instance, field -> !Modifier.isStatic(field.getModifiers()));
	}

	// --------------------------------------------------------------------------------------------

	public static class Provider implements IContainerServiceProvider {
		@Override
		public InstanceContextParam getService(IContainer container) {
			return new InstanceContextParam(container);
		}
	}
}
