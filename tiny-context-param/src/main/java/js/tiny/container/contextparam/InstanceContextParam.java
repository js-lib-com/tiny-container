package js.tiny.container.contextparam;

import java.lang.reflect.Modifier;

import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IInstancePostConstruct;
import js.tiny.container.spi.IManagedClass;

public class InstanceContextParam extends BaseContextParam implements IInstancePostConstruct {
	public InstanceContextParam(IContainer container) {
		super(container);
	}

	@Override
	public int getPriority() {
		return Priority.INJECT.ordinal();
	}

	@Override
	public void postConstructInstance(IManagedClass managedClass, Object instance) {
		processFields(managedClass, instance, field -> !Modifier.isStatic(field.getModifiers()));
	}
}
