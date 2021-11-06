package js.tiny.container.contextparam;

import java.lang.reflect.Modifier;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IClassPostLoadedProcessor;
import js.tiny.container.spi.IManagedClass;

public class ClassContextParam extends BaseContextParam implements IClassPostLoadedProcessor {
	private static Log log = LogFactory.getLog(ClassContextParam.class);

	public ClassContextParam() {
		log.trace("ClassContextParam()");
	}

	@Override
	public Priority getPriority() {
		return Priority.INJECT;
	}

	@Override
	public <T> void onClassPostLoaded(IManagedClass<T> managedClass) {
		processFields(managedClass, null, field -> Modifier.isStatic(field.getModifiers()));
	}
}
