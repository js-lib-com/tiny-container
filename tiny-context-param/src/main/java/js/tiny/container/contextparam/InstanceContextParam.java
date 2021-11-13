package js.tiny.container.contextparam;

import java.lang.reflect.Modifier;

import js.converter.Converter;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IInstancePostConstructProcessor;
import js.tiny.container.spi.IManagedClass;

/**
 * Inject context parameters into instance, non final fields. Field type could be anything for which there is a
 * {@link Converter} registered.
 * 
 * This processor is executed immediately after instance creation but before instance made available to container. Instance
 * fields designed to be initialized should be annotated with the non standard annotation {@link ContextParam}.
 * 
 * @author Iulian Rotaru
 */
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
		processFields(instance, field -> !Modifier.isStatic(field.getModifiers()));
	}
}
