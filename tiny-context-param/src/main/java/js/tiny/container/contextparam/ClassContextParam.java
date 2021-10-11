package js.tiny.container.contextparam;

import java.lang.reflect.Modifier;

import js.tiny.container.spi.IClassPostLoad;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IManagedClass;

class ClassContextParam extends BaseContextParam implements IClassPostLoad {
	protected ClassContextParam(IContainer container) {
		super(container);
	}

	@Override
	public int getPriority() {
		return Priority.INJECT.ordinal();
	}

	@Override
	public void postLoadClass(IManagedClass managedClass) {
		processFields(managedClass, null, field -> Modifier.isStatic(field.getModifiers()));
	}
}
