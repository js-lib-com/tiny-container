package js.tiny.container.contextparam;

import java.lang.reflect.Modifier;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IInstancePostConstructProcessor;
import js.tiny.container.spi.IManagedClass;

public class InstanceContextParam extends BaseContextParam implements IInstancePostConstructProcessor {
	private static Log log = LogFactory.getLog(InstanceContextParam.class);

	public InstanceContextParam() {
		log.trace("InstanceContextParam()");
	}

	@Override
	public Priority getPriority() {
		return Priority.INJECT;
	}

	@Override
	public <T> void onInstancePostConstruct(IManagedClass<T> managedClass, T instance) {
		processFields(managedClass, instance, field -> !Modifier.isStatic(field.getModifiers()));
	}
}
