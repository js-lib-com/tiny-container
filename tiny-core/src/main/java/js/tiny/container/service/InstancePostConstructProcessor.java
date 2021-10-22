package js.tiny.container.service;

import java.util.Collections;

import javax.annotation.PostConstruct;

import js.lang.BugError;
import js.lang.ManagedPostConstruct;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IInstancePostConstructionProcessor;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IServiceMeta;
import js.tiny.container.spi.IServiceMetaScanner;

/**
 * Execute {@link ManagedPostConstruct#postConstruct()} on managed instance. Instance post-construction is executed after
 * initialization and configuration, of course only if managed instance implements {@link ManagedPostConstruct} interface.
 * 
 * @author Iulian Rotaru
 */
public class InstancePostConstructProcessor extends BaseInstanceLifeCycle implements IInstancePostConstructionProcessor, IServiceMetaScanner {
	private static final Log log = LogFactory.getLog(InstancePostConstructProcessor.class);

	private static final String ATTR_POST_CONSTRUCT = "post-construct";

	@Override
	public Priority getPriority() {
		return Priority.CONSTRUCTOR;
	}

	@Override
	public Iterable<IServiceMeta> scanServiceMeta(IManagedClass<?> managedClass) {
		scanLifeCycleInterface(managedClass, ManagedPostConstruct.class, ATTR_POST_CONSTRUCT);
		return Collections.emptyList();
	}

	@Override
	public Iterable<IServiceMeta> scanServiceMeta(IManagedMethod managedMethod) {
		scanLifeCycleAnnotation(managedMethod, PostConstruct.class, ATTR_POST_CONSTRUCT);
		return Collections.emptyList();
	}

	/**
	 * Execute post-construct on managed instance. In order to perform instance post-construction, managed instance should
	 * implement {@link ManagedPostConstruct} interface.
	 * 
	 * @param managedClass managed class,
	 * @param instance instance of given managed class.
	 * @throws BugError if instance post-construction fails due to exception of user defined logic.
	 */
	@Override
	public <T> void onInstancePostConstruction(IManagedClass<T> managedClass, T instance) {
		IManagedMethod method = managedClass.getAttribute(this, ATTR_POST_CONSTRUCT, IManagedMethod.class);
		if (method == null) {
			return;
		}

		log.debug("Post-construct managed instance |%s|", instance.getClass());
		try {
			method.invoke(instance);
		} catch (Throwable t) {
			throw new BugError("Managed instance |%s| post-construct fail: %s", instance, t);
		}
	}
}
