package js.tiny.container.contextparam;

import java.lang.reflect.Modifier;

import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IInstancePostConstructionProcessor;
import js.tiny.container.spi.IManagedClass;

public class InstanceContextParam extends BaseContextParam implements IInstancePostConstructionProcessor {
	public InstanceContextParam(IContainer container) {
		super(container);
	}

	@Override
	public Priority getPriority() {
		return Priority.INJECT;
	}

	@Override
	public void onInstancePostConstruction(IManagedClass managedClass, Object instance) {
		processFields(managedClass, instance, field -> !Modifier.isStatic(field.getModifiers()));
	}
}
