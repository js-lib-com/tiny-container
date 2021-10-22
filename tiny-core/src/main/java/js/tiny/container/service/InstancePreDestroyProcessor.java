package js.tiny.container.service;

import java.util.Collections;

import javax.annotation.PreDestroy;

import js.lang.ManagedPreDestroy;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IInstancePreDestructionProcessor;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IServiceMeta;
import js.tiny.container.spi.IServiceMetaScanner;

class InstancePreDestroyProcessor extends BaseInstanceLifeCycle implements IInstancePreDestructionProcessor, IServiceMetaScanner {
	private static final Log log = LogFactory.getLog(InstancePreDestroyProcessor.class);

	private static final String ATTR_PRE_DESTROY = "pre-destroy";

	@Override
	public Priority getPriority() {
		return Priority.DESTRUCTOR;
	}

	@Override
	public Iterable<IServiceMeta> scanServiceMeta(IManagedClass<?> managedClass) {
		scanLifeCycleInterface(managedClass, ManagedPreDestroy.class, ATTR_PRE_DESTROY);
		return Collections.emptyList();
	}

	@Override
	public Iterable<IServiceMeta> scanServiceMeta(IManagedMethod managedMethod) {
		scanLifeCycleAnnotation(managedMethod, PreDestroy.class, ATTR_PRE_DESTROY);
		return Collections.emptyList();
	}

	@Override
	public <T> void onInstancePreDestruction(IManagedClass<T> managedClass, T instance) {
		IManagedMethod method = managedClass.getAttribute(this, ATTR_PRE_DESTROY, IManagedMethod.class);
		if (method == null) {
			return;
		}

		log.debug("Pre-destroy managed instance |%s|.", instance.getClass());
		try {
			method.invoke(instance);
		} catch (Throwable t) {
			log.dump(String.format("Managed instance |%s| pre-destroy fail:", instance.getClass()), t);
		}
	}
}
