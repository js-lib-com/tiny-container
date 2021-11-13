package js.tiny.container.contextparam;

import java.lang.reflect.Modifier;

import js.converter.Converter;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IClassPostLoadedProcessor;
import js.tiny.container.spi.IManagedClass;

/**
 * Inject context parameters into class static, non final fields. Field type could be anything for which there is a
 * {@link Converter} registered.
 * 
 * This processor is executed immediately after class loading but before class made available to container. Class fields
 * designed to be initialized should be annotated with the non standard annotation {@link ContextParam}.
 * 
 * @author Iulian Rotaru
 */
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
		processFields(managedClass.getImplementationClass(), field -> Modifier.isStatic(field.getModifiers()));
	}
}
