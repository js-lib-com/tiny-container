package js.tiny.container.contextparam;

import java.lang.reflect.Modifier;

import js.tiny.container.spi.IClassPostLoadedProcessor;
import js.tiny.container.spi.IContainer;
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
	public void onClassPostLoaded(IManagedClass managedClass) {
		processFields(managedClass, null, field -> Modifier.isStatic(field.getModifiers()));
	}
}
