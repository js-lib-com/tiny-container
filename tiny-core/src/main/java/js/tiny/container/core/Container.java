package js.tiny.container.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Observer;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.security.PermitAll;
import javax.ejb.Asynchronous;
import javax.ejb.Remote;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import org.omg.PortableInterceptor.Interceptor;

import com.jslib.injector.ProvisionException;

import js.converter.Converter;
import js.lang.BugError;
import js.lang.Config;
import js.lang.ConfigException;
import js.lang.Configurable;
import js.lang.InstanceInvocationHandler;
import js.lang.ManagedLifeCycle;
import js.lang.ManagedPreDestroy;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.cdi.CDI;
import js.tiny.container.service.ConfigurableInstanceProcessor;
import js.tiny.container.service.FlowProcessorsSet;
import js.tiny.container.service.InstanceFieldsInitializationProcessor;
import js.tiny.container.service.InstanceFieldsInjectionProcessor;
import js.tiny.container.service.InstancePostConstructProcessor;
import js.tiny.container.service.InstanceStartupProcessor;
import js.tiny.container.service.LoggerInstanceProcessor;
import js.tiny.container.spi.IClassPostLoadedProcessor;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IContainerServiceProvider;
import js.tiny.container.spi.IContainerStartProcessor;
import js.tiny.container.spi.IInstancePostConstructionProcessor;
import js.tiny.container.spi.IInstancePreDestructionProcessor;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.util.Classes;
import js.util.Params;

/**
 * Container creates managed classes and handle managed instances life cycle, that is, instances creation and caching. Basic
 * container functionality is managed methods invocation, methods that implement application defined logic for dynamic resources
 * generation and services. One can view container as a bridge between servlets and application defined resources and services
 * or as an environment in which an application runs.
 * <p>
 * At is core container deals with managed classes. Container has a pool of managed classes and every managed class has a pool
 * of managed methods. Beside managed life cycle container provides a host of declarative services to managed instances. Anyway,
 * Container class does not implement managed instances services by itself but delegates factories and processors, implemented
 * by js.container package or as plug-ins.
 * 
 * <pre>
 *     +-------------------------------------------------------------------+
 *     | Tiny Container                                                    |
 *     |    +------------+                              +---------------+  | 
 *     |    | Interfaces |                          +---&gt; ManagedMethod |  |
 *     |    +-----^------+       +--------------+   |   +---------------+  |
 *     |          |          +---&gt; ManagedClass +---+                      |
 *     |          |          |   +--------------+   |   +---------------+  |
 *     |    +-----+------+   |                      +---&gt; ManagedMethod |  |
 *  ---O----&gt; Container  +---+                          +---------------+  |
 *     |    +------------+   |                                             |
 *     |                     ~                      +---...                |
 *     |                     |   +--------------+   |                      |
 *     |                     +---&gt; ManagedClass +---+                      |
 *     |                         +--------------+   |                      |
 *     +-------------------------------------------------------------------+
 * </pre>
 * <p>
 * Here is a quick list of services provided, actually orchestrated, by container:
 * <dl>
 * <dt>Declarative implementation binding for managed classes.</dt>
 * <dd>Managed class has interface used by caller to invoke methods on managed instance. Managed instance is create by factory
 * instead of new operator. The actual implementation is declared into class descriptor and bound at container creation.
 * Implementation bind cannot be hot changed.</dd>
 * <dt>Life span management for managed instances, e.g. application, thread or local scope.</dt>
 * <dd>A new managed instance can be created at every request or can be reused from a cache, depending on managed class scope.
 * Instance scope is declared on managed class descriptor.</dd>
 * <dt>External configuration of managed instances from application descriptor.</dt>
 * <dd>If a managed instance implements {@link js.lang.Configurable} interface container takes care to configure it from managed
 * class configuration section. Every managed class has an alias into application descriptor that identify configurable section.
 * </dd>
 * <dt>Managed life cycle for managed instances.</dt>
 * <dd>For managed instances implementing {@link ManagedLifeCycle} interface container invokes post-create and pre-destroy
 * methods after managed instance creation, respective before destroying.</dd>
 * <dt>Declarative transactions, both mutable and immutable.</dt>
 * <dd>A managed class can be marked as transactional, see {@link Transaction} annotation. Container creates a Java Proxy
 * handler and execute managed method inside transaction boundaries.</dd>
 * <dt>Dependency injection for fields and constructor parameters.</dt>
 * <dd>Current container supports two types of dependency injection: field injection using {@link Inject} annotation and
 * constructor injection. Injected instance should be managed instance on its turn or instantiable plain Java class.</dd>
 * <dt>Managed classes static fields initialization from application descriptor.</dt>
 * <dd>Inject value objects declared in application descriptor into static fields from managed class implementation. Container
 * uses {@link Converter} to convert string to value type. Of course static field should not be final.</dd>
 * <dt>Authenticated remote access to managed methods; authentication occurs at method invocation.</dt>
 * <dd>A method annotated with, or owned by a class annotated with {@link Remote} is named net method and is accessible from
 * remote. Remote access is controller by {@link PermitAll} and {@link Private} annotations. A private net method can be
 * accessed only after authentication.</dd>
 * <dt>Declarative asynchronous execution mode for long running logic, executed in separated thread.</dt>
 * <dd>If a managed method is annotated with {@link Asynchronous} container creates a separated thread and execute method
 * asynchronously. Asynchronous method should return void.</dd>
 * <dt>Method invocation listeners. There are interceptors for before, after and around method invocation.</dt>
 * <dd>Invocation listeners provide a naive, but still useful AOP. There is {@link Interceptors} annotation for tagging methods
 * - declaring join points, and related interface to be implemented by interceptors, aka AOP advice. See {@link Interceptor}
 * interface for sample usage.</dd>
 * <dt>Method instrumentation. Uses {@link IInvocationMeter} to monitor method invocations.</dt>
 * <dd>Every managed method has a meter that updates internal counters about execution time, invocation and exceptions count.
 * Invocation meter interface is used to collect counter values. Instrumentation manager, {@link Observer}, collects
 * periodically all managed methods counters and create report on server logger.</dd>
 * </dl>
 * 
 * <h3 id="descriptors">Descriptors</h3>
 * <p>
 * Every application has a descriptor for declarative services and state initialization. Application descriptor is a XML file,
 * with root child elements named <code>sections</code>. Tiny Container uses these <code>sections</code> to group related
 * configurations and here we will focus only on container specific ones.
 * <p>
 * Container adds supplementary semantics to application descriptor: a section element name is known as <code>alias</code> and
 * this alias can be use to refer other section, known as <code>linked</code> section. There are predefined sections, like
 * <code>managed-class</code> and user defined sections, that need to be declared as linked sections into a predefined section,
 * for example <code>processor</code> section.
 * <p>
 * Here are predefined container descriptor sections.
 * <table border="1" style="border-collapse:collapse;" summary="Predefined Sections">
 * <tr>
 * <th>Name
 * <th>Description
 * <th>Linked Section
 * <tr>
 * <td>managed-classes
 * <td>Managed class descriptors.
 * <td>Managed class configuration object.
 * <tr>
 * <td>pojo-classes
 * <td>Plain Java classes static field initialization.
 * <td>List of static fields and respective intial values.
 * <tr>
 * <td>converters
 * <td>Declarative converters.
 * <td>N/A
 * </table>
 * <p>
 * In sample below there is a predefined section <code>managed-class</code> that has element <code>processor</code>. Element is
 * a class descriptor and its name is related to linked section <code>processor</code>, section that is the managed class
 * configuration object. For details about managed classes descriptors see {@link ManagedClass}.
 * 
 * <pre>
 * &lt;managed-classes&gt;
 * 	&lt;processor interface="js.email.Processor" class="js.email.ProcessorImpl" scope="APPLICATION" /&gt;
 * 	...
 * &lt;/managed-classes&gt;
 * 
 * &lt;pojo-classes&gt;
 * 	&lt;email-reader class="com.mobile.EmailReader" /&gt;
 * 	...
 * &lt;/pojo-classes&gt;
 * 
 * &lt;processor repository="/var/www/vhosts/kids-cademy.com/email" files-pattern="*.htm" &gt;
 * 	&lt;property name="mail.transport.protocol" value="smtp" /&gt;
 * 	...
 * &lt;/processor&gt;
 * 
 * &lt;email-reader&gt;
 * 	&lt;static-field name="ACCOUNT" value="john.doe@email.com" /&gt;
 * 	...
 * &lt;/email-reader&gt;
 * 
 * &lt;converters&gt;
 * 	&lt;type class="js.email.MessageID" converter="js.email.MessageIDConverter" /&gt;
 * 	...
 * &lt;/converters&gt;
 * </pre>
 * 
 * For <code>pojo-classes</code> and <code>converters</code> predefined sections see {@link #pojoStaticInitialization(Config)},
 * respective {@link #convertersInitialization(Config)}.
 * 
 * <h3 id="life-cycle">Life Cycle</h3>
 * <p>
 * Container has a managed life cycle on its own. This means it is created automatically, initialized and configured,
 * post-constructed and just before ending its life, pre-destroyed. Accordingly, container's life passes five phases:
 * <ol>
 * <li>Construct container - create factories and processors for instance management but leave classes pool loading for next
 * step, see {@link #Container()},
 * <li>Create managed classes pool - scans application and class descriptors and creates all managed classes described there,
 * see {@link #config(Config)},
 * <li>Start managed classes with managed life cycle - this step should occur after all managed classes were loaded and
 * container is fully initialized, see {@link #start()}; this is because a managed instance may have dependencies on other
 * managed classes for injection,
 * <li>Container is running - this stage has no single entry point; it is the sum of all services container provide while is up
 * and running,
 * <li>Destroy container - destroy and release caches, factories and processors; this is container global clean-up invoked at
 * application unload, see {@link #destroy()}.
 * </ol>
 * 
 * <h3 id="instance-retrieval">Instance Retrieval Algorithm</h3> Instance retrieval is the process of obtaining a managed
 * instance, be it from scope factories caches or fresh created by specialized instance factories.
 * <ol>
 * <li>obtain an instance key based on {@link IManagedClass#getKey()} or instance name,
 * <li>use interface class to retrieve managed class from container classes pool,
 * <li>if managed class not found exit with bug error; if {@link #getOptionalInstance(Class, Object...)} returns null,
 * <li>get scope and instance factories for managed class instance scope and type; if managed class instance scope is local
 * there is no scope factory,
 * <li>if scope factory is null execute local instance factory, that creates a new instance, and returns it; applies arguments
 * processing on local instance but not instance processors,
 * <li>if there is scope factory do next steps into synchronized block,
 * <li>try to retrieve instance from scope factory cache,
 * <li>if no cached instance pre-process arguments, create a new instance using instance factory and persist instance on scope
 * factory,
 * <li>end synchronized block,
 * <li>arguments pre-processing takes care to inject constructor dependencies,
 * <li>if new instance is created execute instance post-processors into registration order.
 * </ol>
 * 
 * @author Iulian Rotaru
 * @version draft
 */
public class Container implements IContainer, Configurable {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(Container.class);

	protected final CDI cdi;

	private final Set<IContainerService> containerServices = new HashSet<>();

	/**
	 * Master cache for all managed classes registered to container. Since an application has one and only one container
	 * instance, managed classes pool is unique per application. This pool is initialized by {@link #config(Config)} method.
	 */
	private final Map<Class<?>, IManagedClass<?>> classesPool = new HashMap<>();

	private final FlowProcessorsSet<IContainerStartProcessor> containerStartProcessors = new FlowProcessorsSet<>();

	/**
	 * Class post-load processors are executed after {@link ManagedClass} creation and generally deals with managed
	 * implementation static fields initialization, but is not limited to.
	 * <p>
	 * These processors are registered by {@link #registerClassPostLoadProcessor(IClassPostLoadedProcessor)}. Note that these
	 * processors are global and executed for ALL managed classes.
	 */
	private final FlowProcessorsSet<IClassPostLoadedProcessor> classPostLoadedProcessors = new FlowProcessorsSet<>();

	/**
	 * Instance post-processors are executed only on newly created managed instances. If instance is reused from scope cache
	 * this processors are not executed. They add instance specific services. This list contains processors in execution order.
	 * <p>
	 * There are a number of built-in processor created by constructor but subclass may register new ones via
	 * {@link #registerInstanceProcessor(IInstancePostConstructionProcessor)}.
	 */
	private final FlowProcessorsSet<IInstancePostConstructionProcessor> instancePostConstructionProcessors = new FlowProcessorsSet<>();

	private final FlowProcessorsSet<IInstancePreDestructionProcessor> instancePreDestructionProcessors = new FlowProcessorsSet<>();

	// --------------------------------------------------------------------------------------------
	// CONTAINER LIFE CYCLE

	public Container() {
		this(CDI.create());
	}

	/**
	 * Create factories and processors for instance retrieval but leave classes pool loading for {@link #config(Config)}. This
	 * constructor creates built-in factories and processors but subclass may add its own.
	 */
	public Container(CDI cdi) {
		this.cdi = cdi;
		this.cdi.bindInstance(IContainer.class, this);

		// load external and built-in container services

		for (IContainerServiceProvider provider : ServiceLoader.load(IContainerServiceProvider.class)) {
			IContainerService service = provider.getService(this);
			log.debug("Load container service |%s|.", service.getClass());
			containerServices.add(service);

			if (service instanceof IContainerStartProcessor) {
				containerStartProcessors.add((IContainerStartProcessor) service);
			}
			if (service instanceof IClassPostLoadedProcessor) {
				classPostLoadedProcessors.add((IClassPostLoadedProcessor) service);
			}
			if (service instanceof IInstancePostConstructionProcessor) {
				instancePostConstructionProcessors.add((IInstancePostConstructionProcessor) service);
			}
			if (service instanceof IInstancePreDestructionProcessor) {
				instancePreDestructionProcessors.add((IInstancePreDestructionProcessor) service);
			}
		}

		containerStartProcessors.add(new InstanceStartupProcessor());

		instancePostConstructionProcessors.add(new InstanceFieldsInjectionProcessor());
		instancePostConstructionProcessors.add(new InstanceFieldsInitializationProcessor());
		instancePostConstructionProcessors.add(new ConfigurableInstanceProcessor());
		instancePostConstructionProcessors.add(new InstancePostConstructProcessor());
		instancePostConstructionProcessors.add(new LoggerInstanceProcessor());
	}

	/**
	 * Create all managed classes registered to this container via external application descriptor. For every found managed
	 * class execute {@link #classPostLoadedProcessors}. After managed classes initialization configure CDI.
	 * 
	 * @param config container configuration object.
	 * @throws ConfigException if container configuration fails.
	 */
	@Override
	public void config(Config config) throws ConfigException {
		log.trace("config(Config)");

		log.debug("Load managed classes from application descriptor.");
		Config managedClassesSection = config.getChild("managed-classes");
		if (managedClassesSection != null) {
			for (Config classDescriptor : managedClassesSection.getChildren()) {
				if (!classDescriptor.hasAttribute("interface")) {
					classDescriptor.setAttribute("interface", classDescriptor.getAttribute("class"));
				}

				ManagedClass<?> managedClass = new ManagedClass<>(this, classDescriptor);
				log.debug("Register managed class |%s|.", managedClass);
				classesPool.put(managedClass.getInterfaceClass(), managedClass);

				classPostLoadedProcessors.forEach(processor -> {
					processor.onClassPostLoaded(managedClass);
				});
			}
		}

		log.debug("Configure CDI.");
		cdi.configure(classesPool.values());
	}

	/** Execute container services registered to {@link #containerStartProcessors}. */
	public void start() {
		log.trace("start()");
		containerStartProcessors.forEach(processor -> {
			processor.onContainerStart(this);
		});
	}

	/**
	 * Destroy container and release caches, factories and processors. This is container global clean-up invoked at application
	 * unload; after executing this method no managed instance can be created or reused. Attempting to use {@link AppFactory}
	 * after container destroy may lead to not predictable behavior.
	 * <p>
	 * Before caches clean-up, this method invokes {@link ManagedPreDestroy} on all managed instances that are active, that is,
	 * cached in all scope factories. Takes care to execute pre-destroy in reverse order from application descriptor
	 * declarations. This ensure {@link App} is destroyed last since App class descriptor is declared first into application
	 * descriptor.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void destroy() {
		log.trace("destroy()");

		// classes pool is not sorted
		// bellow sorted set is used to ensure reverse order on managed classes destruction

		// comparison is based on managed class key that is created incrementally
		// compare second with first to ensure descending sorting
		Set<IManagedClass> sortedClasses = new TreeSet<>((o1, o2) -> o2.getKey().compareTo(o1.getKey()));
		for (IManagedClass managedClass : classesPool.values()) {
			if (!managedClass.getInstanceScope().isLOCAL()) {
				sortedClasses.add(managedClass);
			}
		}

		for (IManagedClass managedClass : sortedClasses) {
			Object instance = cdi.getScopeInstance(managedClass.getInterfaceClass());
			if (instance == null) {
				log.debug("Cannot obtain instance for pre-destroy method for class |%s|.", managedClass);
				continue;
			}

			// sorted managed classes contains only implementations of pre-destroy interface
			// in case instance is a Java Proxy takes care to execute pre-destroy hook on wrapped instance
			// in order to avoid adding container services to this finalization hook
			if (instance instanceof InstanceInvocationHandler) {
				instance = Classes.unproxy(instance);
			}
			log.debug("Pre-destroy managed instance |%s|.", instance.getClass());

			final Object finalInstance = instance;
			instancePreDestructionProcessors.forEach(processor -> {
				processor.onInstancePreDestruction(managedClass, finalInstance);
			});
		}

		for (IContainerService containerService : containerServices) {
			containerService.destroy();
		}

		classesPool.clear();
		classPostLoadedProcessors.clear();
		instancePostConstructionProcessors.clear();
		instancePreDestructionProcessors.clear();
	}

	// --------------------------------------------------------------------------------------------

	@Override
	public <T> T getInstance(Class<? super T> interfaceClass) {
		Params.notNull(interfaceClass, "Interface class");

		@SuppressWarnings("unchecked")
		IManagedClass<T> managedClass = (IManagedClass<T>) classesPool.get(interfaceClass);
		if (managedClass == null) {
			throw new BugError("No managed class associated with interface class |%s|.", interfaceClass);
		}

		return getInstance(managedClass);
	}

	@Override
	public <T> T getOptionalInstance(Class<? super T> interfaceClass) {
		Params.notNull(interfaceClass, "Interface class");

		@SuppressWarnings("unchecked")
		IManagedClass<T> managedClass = (IManagedClass<T>) classesPool.get(interfaceClass);
		if (managedClass == null) {
			return null;
		}

		// here is a piece of code that uses exception for normal logic flow but I do not see alternative
		// ServiceInstanceFactory should throw exception that propagates to AppFactory and application code
		// on the other hand this getOptionalInstance() should return null for missing service provider

		try {
			return getInstance(managedClass);
		} catch (ProvisionException e) {
			// log record is not an error since exception is expected
			log.debug(e);
			return null;
		}
	}

	@Override
	public <T> T getInstance(IManagedClass<T> managedClass) {
		return cdi.getInstance(managedClass.getInterfaceClass(), (instanceManagedClass, instance) -> {
			instancePostConstructionProcessors.forEach(processor -> {
				processor.onInstancePostConstruction(instanceManagedClass, instance);
			});
		});
	}

	// ----------------------------------------------------
	// CONTAINER SPI

	@Override
	public Iterable<IManagedClass<?>> getManagedClasses() {
		return classesPool.values();
	}

	@Override
	public boolean isManagedClass(Class<?> interfaceClass) {
		return classesPool.containsKey(interfaceClass);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> IManagedClass<T> getManagedClass(Class<T> interfaceClass) {
		return (IManagedClass<T>) classesPool.get(interfaceClass);
	}

	@Override
	public Iterable<IManagedMethod> getManagedMethods() {
		return new Iterable<IManagedMethod>() {
			@Override
			public Iterator<IManagedMethod> iterator() {
				return new ManagedMethodsIterator();
			}
		};
	}

	// --------------------------------------------------------------------------------------------
	// INNER CLASSES

	/**
	 * Iterator for all managed methods defined by this container. Traverses all managed methods from all managed classes from
	 * {@link Container#classesPool}. There is no guarantee for a particular order.
	 * 
	 * @author Iulian Rotaru
	 */
	private class ManagedMethodsIterator implements Iterator<IManagedMethod> {
		/** Managed classes iterator. */
		private final Iterator<IManagedClass<?>> classesIterator;

		/** Iterator on managed methods from current managed class. */
		private Iterator<IManagedMethod> currentMethodsIterator;

		/**
		 * Initialize iterators for managed classes and current class methods.
		 */
		public ManagedMethodsIterator() {
			classesIterator = classesPool.values().iterator();
			if (!classesIterator.hasNext()) {
				throw new BugError("Empty classes pool.");
			}
			currentMethodsIterator = nextMethodIterator();
		}

		@Override
		public boolean hasNext() {
			while (!currentMethodsIterator.hasNext()) {
				if (!classesIterator.hasNext()) {
					return false;
				}
				currentMethodsIterator = nextMethodIterator();
			}
			return true;
		}

		@Override
		public IManagedMethod next() {
			return currentMethodsIterator.next();
		}

		private Iterator<IManagedMethod> nextMethodIterator() {
			return classesIterator.next().getManagedMethods().iterator();
		}
	}

	// --------------------------------------------------------------------------------------------

	public void config(IManagedClass<?>... managedClasses) {
		for (IManagedClass<?> managedClass : managedClasses) {
			classesPool.put(managedClass.getInterfaceClass(), managedClass);
		}
	}

	Map<Class<?>, IManagedClass<?>> classesPool() {
		return classesPool;
	}

	Collection<IContainerService> getServices() {
		return containerServices;
	}
}
