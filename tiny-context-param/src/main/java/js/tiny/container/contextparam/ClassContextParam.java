package js.tiny.container.contextparam;

import java.lang.reflect.Modifier;

import js.tiny.container.spi.IClassPostLoadedProcessor;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerServiceProvider;
import js.tiny.container.spi.IManagedClass;

class ClassContextParam extends BaseContextParam implements IClassPostLoadedProcessor {
	protected ClassContextParam(IContainer container) {
		super(container);
	}

	@Override
	public Priority getPriority() {
		return Priority.INJECT;
	}

	@Override
	public <T> void onClassPostLoaded(IManagedClass<T> managedClass) {
		processFields(managedClass, null, field -> Modifier.isStatic(field.getModifiers()));
	}

	public static class Provider implements IContainerServiceProvider {
		@Override
		public ClassContextParam getService(IContainer container) {
			return new ClassContextParam(container);
		}
	}
}
