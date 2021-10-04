package js.tiny.container;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.security.PermitAll;
import javax.ejb.Asynchronous;
import javax.ejb.Remote;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import js.converter.Converter;
import js.converter.ConverterException;
import js.converter.ConverterRegistry;
import js.lang.BugError;
import js.lang.Config;
import js.lang.ConfigException;
import js.lang.Configurable;
import js.lang.InstanceInvocationHandler;
import js.lang.InvocationException;
import js.lang.ManagedLifeCycle;
import js.lang.ManagedPreDestroy;
import js.lang.NoProviderException;
import js.log.Log;
import js.log.LogFactory;
import js.rmi.RemoteFactory;
import js.tiny.container.core.App;
import js.tiny.container.core.AppContext;
import js.tiny.container.core.AppFactory;
import js.tiny.container.spi.IClassPostProcessor;
import js.tiny.container.spi.IContainer;
import js.tiny.container.spi.IContainerService;
import js.tiny.container.spi.IInstancePostProcessor;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
//import js.tiny.container.timer.CalendarTimerService;
import js.transaction.Transaction;
import js.util.Classes;
import js.util.Params;
import js.util.Types;

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
 * Instance scope is declared on managed class descriptor. New managed instances are created by {@link InstanceFactory} whereas
 * life span is controlled by {@link ScopeFactory} class.</dd>
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
 * <dt>Method instrumentation. Uses {@link InvocationMeter} to monitor method invocations.</dt>
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
public abstract class Container implements IContainer, Configurable {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(Container.class);

	/**
	 * Scope specific factories mapped to related {@link InstanceScope}. A scope factory takes care of managed instance life
	 * span and reuse it if instance is retrieved in the same scope. It is part of <a href="#instance-retrieval">Instance
	 * Retrieval Algorithm</a>.
	 * <p>
	 * There are a number of built-in scope factories created by constructor but subclass may register new ones via
	 * {@link #registerScopeFactory(ScopeFactory)}.
	 */
	private final Map<InstanceScope, ScopeFactory> scopeFactories = new HashMap<>();

	/**
	 * Mutex for scope factory access. Instance retrieval algorithm uses scope factories in two steps: first try to retrieve an
	 * instance already existing on scope and if null persist a newly created one. These steps are synchronized using this
	 * mutex.
	 */
	private final Object scopeMutex = new Object();

	/**
	 * Instance factories mapped to related {@link InstanceType}. An instance factory deals with actual instance creation and is
	 * enacted by <a href="#instance-retrieval">Instance Retrieval Algorithm</a> only if on current scope there is not an
	 * already created managed instance.
	 * <p>
	 * There are a number of built-in instance factories created by constructor but subclass may register new ones via
	 * {@link #registerInstanceFactory(InstanceType, InstanceFactory)}.
	 */
	private final Map<InstanceType, InstanceFactory> instanceFactories = new HashMap<>();

	/**
	 * Class post-processors are executed after {@link ManagedClass} creation and generally deals with managed implementation
	 * static fields initialization, but is not limited to.
	 * <p>
	 * These processors are registered by {@link #registerClassProcessor(IClassPostProcessor)}. Note that these processors are global
	 * and executed for ALL managed classes.
	 */
	private final List<IClassPostProcessor> classProcessors = new ArrayList<>();

	/**
	 * Instance post-processors are executed only on newly created managed instances. If instance is reused from scope cache
	 * this processors are not executed. They add instance specific services. This list contains processors in execution order.
	 * <p>
	 * There are a number of built-in processor created by constructor but subclass may register new ones via
	 * {@link #registerInstanceProcessor(IInstancePostProcessor)}.
	 */
	private final List<IInstancePostProcessor> instanceProcessors = new ArrayList<>();

	/**
	 * Processor for managed constructor and method invocation arguments. Takes care of dependency injection on arguments and
	 * some insanity checks.
	 */
	private final ArgumentsProcessor argumentsProcessor = new ArgumentsProcessor();

	/**
	 * Master cache for all managed classes registered to container. Since an application has one and only one container
	 * instance, managed classes pool is unique per application. This pool is initialized by {@link #config(Config)} method.
	 */
	private final Map<Class<?>, IManagedClass> classesPool = new HashMap<>();

	private final List<IContainerService> containerServices = new ArrayList<>();

	// --------------------------------------------------------------------------------------------
	// CONTAINER LIFE CYCLE

	/**
	 * Create factories and processors for instance retrieval but leave classes pool loading for {@link #config(Config)}. This
	 * constructor creates built-in factories and processors but subclass may add its own.
	 */
	public Container() {
		log.trace("Container()");

		for (IContainerService containerService : ServiceLoader.load(IContainerService.class)) {
			log.debug("Load container service |%s|.", containerService.getClass());
			containerServices.add(containerService);
		}

		// first register plug-in scope factories in order to avoid overriding built-in factories
		for (ScopeFactory scopeFactory : ServiceLoader.load(ScopeFactory.class)) {
			registerScopeFactory(scopeFactory);
		}
		registerScopeFactory(new ApplicationScopeFactory());
		registerScopeFactory(new ThreadScopeFactory());
		// local instances have no managed scope; simple creates a new instance for every call
		registerScopeFactory(null);

		// first register plug-in instance factories in order to avoid overriding built-in factories
		for (InstanceFactory instanceFactory : ServiceLoader.load(InstanceFactory.class)) {
			registerInstanceFactory(instanceFactory.getInstanceType(), instanceFactory);
		}

		InstanceFactory localFactory = new LocalInstanceFactory();
		registerInstanceFactory(InstanceType.POJO, localFactory);
		registerInstanceFactory(InstanceType.PROXY, localFactory);

		registerInstanceFactory(InstanceType.SERVICE, new ServiceInstanceFactory());
		registerInstanceFactory(InstanceType.REMOTE, new RemoteInstanceFactory());

		for (IContainerService containerService : containerServices) {
			if (containerService instanceof IInstancePostProcessor) {
				registerInstanceProcessor((IInstancePostProcessor) containerService);
			}
		}

		registerInstanceProcessor();
		registerClassProcessor();
	}

	public List<IContainerService> getServices() {
		return containerServices;
	}

	/**
	 * Register all instance post-processors.
	 */
	protected void registerInstanceProcessor() {
		registerInstanceProcessor(new InstanceFieldsInjectionProcessor());
		registerInstanceProcessor(new InstanceFieldsInitializationProcessor());
		registerInstanceProcessor(new ConfigurableInstanceProcessor());
		registerInstanceProcessor(new PostConstructInstanceProcessor());
		registerInstanceProcessor(new LoggerInstanceProcessor());

	}

	/**
	 * Register all managed class post-processors.
	 */
	protected void registerClassProcessor() {
	}

	/**
	 * Register scope factory for the instance scope returned by {@link ScopeFactory#getInstanceScope()}.
	 * 
	 * @param scopeFactory scope factory instance, possible null for local scope.
	 * @throws BugError if instance scope is already registered.
	 */
	protected void registerScopeFactory(ScopeFactory scopeFactory) {
		if (scopeFactory == null) {
			log.debug("Register null scope factory to |%s|.", InstanceScope.LOCAL);
			scopeFactories.put(InstanceScope.LOCAL, null);
			return;
		}

		final InstanceScope instanceScope = scopeFactory.getInstanceScope();
		log.debug("Register scope factory |%s| to |%s|.", scopeFactory.getClass(), instanceScope);
		if (scopeFactories.put(instanceScope, scopeFactory) != null) {
			throw new BugError("Attempt to override instance scope |%s|.", instanceScope);
		}
	}

	/**
	 * Register instance factory to requested instance type.
	 * 
	 * @param instanceType instance type used as key for instance factory,
	 * @param instanceFactory instance factory.
	 * @throws BugError if instance type is already registered.
	 */
	protected void registerInstanceFactory(InstanceType instanceType, InstanceFactory instanceFactory) {
		log.debug("Register instance factory |%s| to |%s|.", instanceFactory.getClass(), instanceType);
		if (instanceFactories.put(instanceType, instanceFactory) != null) {
			throw new BugError("Attempt to override instance type |%s|.", instanceType);
		}
	}

	/**
	 * Register instance processor. Only a single instance per processor class is allowed.
	 * 
	 * @param instanceProcessor instance processor.
	 * @throws BugError if instance processor class is already registered.
	 * @see #instanceProcessors
	 */
	protected void registerInstanceProcessor(IInstancePostProcessor instanceProcessor) {
		for (IInstancePostProcessor existingInstanceProcessoor : instanceProcessors) {
			if (existingInstanceProcessoor.getClass().equals(instanceProcessor.getClass())) {
				throw new BugError("Attempt to override instance processor |%s|.", instanceProcessor.getClass());
			}
		}
		log.debug("Register instance processor |%s|.", instanceProcessor.getClass());
		instanceProcessors.add(instanceProcessor);
	}

	/**
	 * Register global processors for managed classes. Post-processors are singletons and only one post-processor instance of a
	 * type is allowed.
	 * 
	 * @param classProcessor managed class post-processor.
	 * @throws BugError if class processor class is already registered.
	 * @see #classProcessors
	 */
	protected void registerClassProcessor(IClassPostProcessor classProcessor) {
		for (IClassPostProcessor existingClassProcessoor : classProcessors) {
			if (existingClassProcessoor.getClass().equals(classProcessor.getClass())) {
				throw new BugError("Attempt to override class processor |%s|.", classProcessor.getClass());
			}
		}
		log.debug("Register class processor |%s|.", classProcessor.getClass());
		classProcessors.add(classProcessor);
	}

	/**
	 * Create all managed instances registered to this container via external application descriptor. See
	 * <a href="#descriptors">Descriptors</a> for details about application and class descriptors.
	 * 
	 * @param config container configuration object.
	 * @throws ConfigException if container configuration fails.
	 * @throws BugError for insane condition that prevent managed classes initialization.
	 */
	@Override
	public void config(Config config) throws ConfigException {
		log.trace("config(Config)");

		// normalized class descriptors with overrides resolves; should preserve order from external files

		// it is legal for application descriptor to override already defined class descriptor
		// for example user defined ro.gnotis.Fax2MailApp overrides library declared js.core.App
		// <app interface='js.core.App' class='ro.gnotis.Fax2MailApp' />

		// when found an overridden class descriptor replace it with most recent version
		// only class descriptors with single interface can be overridden

		List<Config> classDescriptors = new ArrayList<>();

		for (Config descriptorsSection : config.findChildren("managed-classes", "web-sockets")) {
			CLASS_DESCRIPTORS: for (Config classDescriptor : descriptorsSection.getChildren()) {
				if (!classDescriptor.hasChildren()) {
					if (!classDescriptor.hasAttribute("interface")) {
						classDescriptor.setAttribute("interface", classDescriptor.getAttribute("class"));
					}
					String interfaceClass = classDescriptor.getAttribute("interface");
					for (int i = 0; i < classDescriptors.size(); ++i) {
						if (classDescriptors.get(i).hasAttribute("interface", interfaceClass)) {
							log.debug("Override class descriptor for interface |%s|.", interfaceClass);
							classDescriptors.set(i, classDescriptor);
							continue CLASS_DESCRIPTORS;
						}
					}
				}
				classDescriptors.add(classDescriptor);
			}
		}

		// second step is to actually populate the classes pool from normalized class descriptors list

		for (Config classDescriptor : classDescriptors) {
			// create managed class, a single one per class descriptor, even if there are multiple interfaces
			// if multiple interfaces register the same managed class multiple times, once per interface
			// this way, no mater which interface is used to retrieve the instance it uses in the end the same managed class
			ManagedClass managedClass = new ManagedClass(this, classDescriptor);
			log.debug("Register managed class |%s|.", managedClass);
			for (Class<?> interfaceClass : managedClass.getInterfaceClasses()) {
				classesPool.put(interfaceClass, managedClass);
			}

			for (IClassPostProcessor classProcessor : classProcessors) {
				classProcessor.postProcessClass(managedClass);
			}
		}

		convertersInitialization(config);
		pojoStaticInitialization(config);

		// special handling for this container instance accessed via application context
		// need to ensure this container instance is reused and not to create a new one
		IManagedClass appContext = classesPool.get(AppContext.class);
		// application context can be null on tests
		// also on tests application context interface can be implemented by mock class not in container hierarchy
		if (appContext != null && Types.isKindOf(appContext.getImplementationClass(), IContainer.class)) {
			log.debug("Persist container instance on application scope.");
			// managed class key cannot be null
			scopeFactories.get(InstanceScope.APPLICATION).persistInstance(new InstanceKey(appContext.getKey().toString()), this);
		}
	}

	/**
	 * Ensure all managed classes marked with 'auto-creation' are instantiated. Invoked at a final stage of container
	 * initialization, this method checks every managed class that has {@link IManagedClass#isAutoInstanceCreation()} flag set
	 * and ensure is instantiated.
	 * <p>
	 * Takes care to instantiate, configure if the case, and execute post-construct in the order from application descriptor.
	 * This is critical for assuring that {@link App} is created first; {@link App} class descriptor is declared first into
	 * application descriptor.
	 * <p>
	 * Note that this method does not explicitly execute {@link ManagedLifeCycle#postConstruct()} hooks; this hooks are actually
	 * executed by instance processor, see {link {@link PostConstructInstanceProcessor}.
	 */
	@SuppressWarnings("unchecked")
	public void start() {
		log.trace("start()");

		// classes pool is not sorted; it is a hash map that does not guarantee order
		// also, a managed class may appear multiple times if have multiple interfaces
		// bellow sorted set is used to ensure ascending order on managed classes instantiation

		// comparison is based on managed class key that is created incrementally

		Set<IManagedClass> sortedClasses = new TreeSet<>(new Comparator<IManagedClass>() {
			@Override
			public int compare(IManagedClass o1, IManagedClass o2) {
				// compare first with second to ensure ascending sorting
				return o1.getKey().compareTo(o2.getKey());
			}
		});

		for (IManagedClass managedClass : classesPool.values()) {
			// process only implementations of managed life cycle interface
			if (managedClass.isAutoInstanceCreation()) {
				sortedClasses.add(managedClass);
			}
		}

		for (IManagedClass managedClass : sortedClasses) {
			// call getInstance to ensure managed instance with managed life cycle is started
			// if there are more than one single interface peek one, no matter which; the simple way is to peek the first
			// getInstance() will create instance only if not already exist; returned value is ignored

			log.debug("Create managed instance with managed life cycle |%s|.", managedClass.getInterfaceClass());
			getInstance((Class<? super Object>) managedClass.getInterfaceClass());
		}
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
	public void destroy() {
		log.trace("destroy()");

		// !!! timer service should be destroyed first to avoid invoking timer methods on cleaned managed instance
		for (IContainerService containerService : containerServices) {
			containerService.destroy();
		}

		// classes pool is not sorted; it is a hash map for performance reasons
		// also, a managed class may appear multiple times if have multiple interfaces
		// bellow sorted set is used to ensure reverse order on managed classes destruction
		// comparison is based on managed class key that is created incrementally

		Set<IManagedClass> sortedClasses = new TreeSet<>(new Comparator<IManagedClass>() {
			@Override
			public int compare(IManagedClass o1, IManagedClass o2) {
				// compare second with first to ensure descending sorting
				return o2.getKey().compareTo(o1.getKey());
			}
		});

		for (IManagedClass managedClass : classesPool.values()) {
			// process only managed classes with pre-destroy hook
			if (managedClass.getPreDestroyMethod() != null) {
				sortedClasses.add(managedClass);
			}
		}

		for (IManagedClass managedClass : sortedClasses) {
			IManagedMethod preDestroyMethod = managedClass.getPreDestroyMethod();

			ScopeFactory scopeFactory = scopeFactories.get(managedClass.getInstanceScope());
			// managed class key cannot be null
			InstanceKey instanceKey = new InstanceKey(managedClass.getKey().toString());
			Object instance = scopeFactory.getInstance(instanceKey);
			if (instance == null) {
				log.debug("Cannot obtain instance for pre-destroy method |%s|.", preDestroyMethod);
				continue;
			}

			// sorted managed classes contains only implementations of pre-destroy interface
			// in case instance is a Java Proxy takes care to execute pre-destroy hook on wrapped instance
			// in order to avoid adding container services to this finalization hook
			instance = Classes.unproxy(instance);
			log.debug("Pre-destroy managed instance |%s|.", instance.getClass());

			try {
				preDestroyMethod.invoke(instance);
			} catch (Throwable t) {
				log.dump(String.format("Managed instance |%s| pre-destroy fail:", instance.getClass()), t);
			}
		}

		for (ScopeFactory scopeFactory : scopeFactories.values()) {
			// local scope has null factory
			if (scopeFactory != null) {
				scopeFactory.clear();
			}
		}

		classesPool.clear();
		instanceProcessors.clear();
		scopeFactories.clear();
		instanceFactories.clear();
	}

	// --------------------------------------------------------------------------------------------
	// INSTANCE RETRIEVAL ALGORITHM

	@Override
	public <T> T getInstance(Class<? super T> interfaceClass, Object... args) {
		Params.notNull(interfaceClass, "Interface class");

		IManagedClass managedClass = classesPool.get(interfaceClass);
		if (managedClass == null) {
			throw new BugError("No managed class associated with interface class |%s|.", interfaceClass);
		}

		// managed class key cannot be null
		InstanceKey instanceKey = new InstanceKey(managedClass.getKey().toString());
		return getInstance(managedClass, instanceKey, args);
	}

	@Override
	public <T> T getInstance(String instanceName, Class<? super T> interfaceClass, Object... args) {
		Params.notNullOrEmpty(instanceName, "Instance name");
		Params.notNull(interfaceClass, "Interface class");

		IManagedClass managedClass = classesPool.get(interfaceClass);
		if (managedClass == null) {
			throw new BugError("No managed class associated with interface class |%s|.", interfaceClass);
		}

		// instance name should be unique and can be used as uniqueness indicator
		InstanceKey key = new InstanceKey(instanceName);
		return getInstance(managedClass, key, args);
	}

	@Override
	public <T> T getOptionalInstance(Class<? super T> interfaceClass, Object... args) {
		Params.notNull(interfaceClass, "Interface class");

		IManagedClass managedClass = classesPool.get(interfaceClass);
		if (managedClass == null) {
			return null;
		}

		// here is a piece of code that uses exception for normal logic flow but I do not see alternative
		// ServiceInstanceFactory should throw exception that propagates to AppFactory and application code
		// on the other hand this getOptionalInstance() should return null for missing service provider

		// managed class key cannot be null
		InstanceKey instanceKey = new InstanceKey(managedClass.getKey().toString());
		try {
			return getInstance(managedClass, instanceKey, args);
		} catch (NoProviderException e) {
			// is not an error since application code is prepared for missing provider
			log.debug(e);
			return null;
		}
	}

	@Override
	public <T> T getInstance(IManagedClass managedClass, Object... args) {
		// managed class key cannot be null
		InstanceKey instanceKey = new InstanceKey(managedClass.getKey().toString());
		return getInstance(managedClass, instanceKey, args);
	}

	/**
	 * Utility method that implements the core of managed instances retrieval algorithm. This method gets existing managed
	 * instance from container caches or creates a new instance. If instance is fresh created add instance services via
	 * registered instance processors.
	 * <p>
	 * Here is managed instance retrieval algorithm part implemented by this method.
	 * <ol>
	 * <li>get scope and instance factories for managed class instance scope and type; if managed class instance scope is local
	 * there is no scope factory,
	 * <li>if scope factory is null execute local instance factory, that creates a new instance, and returns it; applies
	 * arguments processing on local instance but not instance processors,
	 * <li>if there is scope factory do next steps into synchronized block,
	 * <li>try to retrieve instance from scope factory cache,
	 * <li>if no cached instance pre-process arguments, create a new instance using instance factory and persist instance on
	 * scope factory,
	 * <li>end synchronized block,
	 * <li>arguments pre-processing takes care to inject constructor dependencies,
	 * <li>if new instance is created execute instance post-processors into registration order.
	 * </ol>
	 * 
	 * @param managedClass managed class for which instance is to be retrieved,
	 * @param instanceKey managed instance key,
	 * @param args optional constructor arguments, used only if new local instance is created.
	 * @param <T> managed instance type.
	 * @return managed instance, created on the fly or reused from caches, but never null.
	 * 
	 * @throws InvocationException if instance is local and constructor fails.
	 * @throws NoProviderException if interface is a service and no provider found on run-time.
	 * @throws ConverterException if attempt to initialize a field with a type for which there is no converter,
	 * @throws BugError if dependency value cannot be created or circular dependency is detected.
	 * @throws BugError if instance configuration fails either due to bad configuration object or fail on configuration user
	 *             defined logic.
	 * @throws BugError if instance post-construction fails due to exception of user defined logic.
	 * @throws BugError if attempt to assign field to not POJO type.
	 */
	@SuppressWarnings("unchecked")
	private <T> T getInstance(IManagedClass managedClass, InstanceKey instanceKey, Object... args) {
		// if(managedClass.getInstanceType().isREMOTE()) {
		// return getRemoteInstance(new URL(managedClass.getImplementationURL()), (Class<? super T>)
		// managedClass.getInterfaceClass());
		// }

		ScopeFactory scopeFactory = scopeFactories.get(managedClass.getInstanceScope());
		InstanceFactory instanceFactory = instanceFactories.get(managedClass.getInstanceType());

		if (scopeFactory == null) {
			args = argumentsProcessor.preProcessArguments(managedClass, args);
			return instanceFactory.newInstance(managedClass, args);
		}

		boolean postProcessingEnabled = false;
		Object instance = null;
		synchronized (scopeMutex) {
			instance = scopeFactory.getInstance(instanceKey);
			if (instance == null) {
				postProcessingEnabled = true;
				args = argumentsProcessor.preProcessArguments(managedClass, args);
				instance = instanceFactory.newInstance(managedClass, args);
				scopeFactory.persistInstance(instanceKey, instance);
			}
		}

		if (!postProcessingEnabled) {
			return (T) instance;
		}

		// post-processors operate on bare POJO instances but is possible for instance factory to return a Java Proxy
		// if instance is a Java Proxy that uses InstanceInvocationHandler extract wrapped POJO instance
		// if instance is a Java Proxy that does not use InstanceInvocationHandler post-processing is not performed at all
		// if instance is not a Java Proxy execute post-processing on it

		Object pojoInstance = null;
		if (instance instanceof Proxy) {
			if (Proxy.getInvocationHandler(instance) instanceof InstanceInvocationHandler) {
				InstanceInvocationHandler<T> handler = (InstanceInvocationHandler<T>) Proxy.getInvocationHandler(instance);
				pojoInstance = handler.getWrappedInstance();
			}
		} else {
			pojoInstance = instance;
		}

		if (pojoInstance != null) {
			for (IInstancePostProcessor instanceProcessor : instanceProcessors) {
				instanceProcessor.postProcessInstance(managedClass, pojoInstance);
			}
		}

		return (T) instance;
	}

	@Override
	public <T> T getRemoteInstance(String implementationURL, Class<? super T> interfaceClass) {
		Params.notNull(implementationURL, "Implementation URL");
		Params.notNull(interfaceClass, "Interface class");
		Params.isTrue(interfaceClass.isInterface(), "Interface class argument is not actually an interface.");

		InstanceFactory instanceFactory = instanceFactories.get(InstanceType.REMOTE);
		if (instanceFactory == null) {
			throw new BugError("No REMOTE instance factory registered.");
		}
		if (!(instanceFactory instanceof RemoteFactory)) {
			throw new BugError("Invalid REMOTE instance factory provider. Does not implement %s interface.", RemoteFactory.class);
		}
		return ((RemoteFactory) instanceFactory).getRemoteInstance(implementationURL, interfaceClass);
	}

	@Override
	public <T> T loadService(Class<T> serviceInterface) {
		return Classes.loadService(serviceInterface);
	}

	// ----------------------------------------------------
	// CONTAINER SPI

	@Override
	public Iterable<IManagedClass> getManagedClasses() {
		return classesPool.values();
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

	@Override
	public boolean isManagedClass(Class<?> interfaceClass) {
		return classesPool.containsKey(interfaceClass);
	}

	@Override
	public IManagedClass getManagedClass(Class<?> interfaceClass) {
		return classesPool.get(interfaceClass);
	}

	// --------------------------------------------------------------------------------------------
	// SECURITY CONTEXT INTERFACE

	@Override
	public boolean isAuthenticated() {
		return true;
	}

	@Override
	public boolean isAuthorized(String... roles) {
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// PACKAGE LEVEL METHODS

	/**
	 * Test if instance scope has a {@link ScopeFactory} registered on this container.
	 * 
	 * @param instanceScope instance scope to test for factory registration.
	 * @return true if instance scope has factory.
	 */
	boolean hasScopeFactory(InstanceScope instanceScope) {
		return scopeFactories.containsKey(instanceScope);
	}

	/**
	 * Test if instance type has a {@link InstanceFactory} registered on this container.
	 * 
	 * @param instanceType instance type to test for factory registration.
	 * @return true if instance type has factory.
	 */
	boolean hasInstanceFactory(InstanceType instanceType) {
		return instanceFactories.containsKey(instanceType);
	}

	// --------------------------------------------------------------------------------------------
	// INNER CLASSES

	/**
	 * Iterator for all managed methods defined by this container. Traverses all managed methods from all managed classes from
	 * {@link Container#classesPool}. There is no guarantee for a particular order.
	 * 
	 * @author Iulian Rotaru
	 * @version final
	 */
	private class ManagedMethodsIterator implements Iterator<IManagedMethod> {
		/** Managed classes iterator. */
		private final Iterator<IManagedClass> classesIterator;

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
	// UTILITY METHODS

	/**
	 * Declarative converters registration. This utility scans for <code>converters</code> section into given configuration
	 * object and register converters via {@link ConverterRegistry#registerConverter(Class, Class)}.
	 * <p>
	 * Configuration converter section should respect below syntax.
	 * 
	 * <pre>
	 *  &lt;converters&gt;
	 *      &lt;type class="java.io.File" converter="js.converter.FileConverter" /&gt;
	 *      &lt;type class="java.net.URL" converter="js.converter.UrlConverter" /&gt;
	 *      . . .
	 *      &lt;type class="java.util.TimeZone" converter="js.converter.TimeZoneConverter" /&gt;
	 *  &lt;/converters&gt;
	 * </pre>
	 * 
	 * @param config configuration object.
	 * @throws ConfigException if declared <code>class</code> or <code>converter</code> not found on run-time class path.
	 */
	private static void convertersInitialization(Config config) throws ConfigException {
		Config section = config.getChild("converters");
		if (section == null) {
			return;
		}
		for (Config el : section.findChildren("type")) {
			String className = el.getAttribute("class");
			Class<?> valueType = Classes.forOptionalName(className);
			if (valueType == null) {
				throw new ConfigException("Invalid converter configuration. Value type class |%s| not found.", className);
			}

			String converterName = el.getAttribute("converter");
			Class<? extends Converter> converterClass = Classes.forOptionalName(converterName);
			if (converterClass == null) {
				throw new ConfigException("Invalid converter configuration. Converter class |%s| not found.", converterName);
			}

			ConverterRegistry.getInstance().registerConverter(valueType, converterClass);
		}
	}

	/**
	 * Plain Java objects static initialization. Inject static fields value into arbitrary Java classes. Note that this
	 * mechanism is not related to managed classes; it acts on regular Java classes and only on static fields.
	 * <p>
	 * Here is a sample configuration for Java objects static injection. There is <code>pojo-classes</code> section that list
	 * all involved classes, with aliases. For every Java class alias there is a section with the same name that has
	 * <code>static</code> name / value elements related by name to class static fields.
	 * 
	 * <pre>
	 * &lt;pojo-classes&gt;
	 * 	&lt;email-reader class="com.mobile.EmailReader" /&gt;
	 * &lt;/pojo-classes&gt;
	 * ...
	 * &lt;email-reader&gt;
	 * 	&lt;static-field name="USER_NAME" value="johndoe" /&gt;
	 * 	&lt;static-field name="PASSWORD" value="secret" /&gt;
	 * &lt;/email-reader&gt;
	 * </pre>
	 * <p>
	 * Static values from above configuration sample are injected into class static fields with the same name. String value from
	 * configuration object is converted to field type.
	 * 
	 * <pre>
	 * public class EmailReader {
	 * 	private static String USER_NAME;
	 * 	private static String PASSWORD;
	 * 	...
	 * }
	 * </pre>
	 * 
	 * This initializer has side effects: it loads configured Java classes at container startup.
	 * 
	 * @param config configuration object.
	 * @throws ConfigException if configuration object is not valid or class or field not found.
	 * @throws ConverterException if there is no converter registered for a specific field type.
	 */
	private static void pojoStaticInitialization(Config config) throws ConfigException {
		Config pojoClassesSection = config.getChild("pojo-classes");
		if (pojoClassesSection == null) {
			return;
		}

		for (Config pojoClassElement : pojoClassesSection.getChildren()) {
			String pojoClassName = pojoClassElement.getAttribute("class");
			if (pojoClassName == null) {
				throw new ConfigException("Invalid POJO class element. Missing <class> attribute.");
			}
			Config configSection = config.getChild(pojoClassElement.getName());
			Class<?> pojoClass = Classes.forOptionalName(pojoClassName);
			if (pojoClass == null) {
				throw new ConfigException("Missing configured POJO class |%s|.", pojoClassName);
			}
			if (configSection == null) {
				continue;
			}

			for (Config staticElement : configSection.findChildren("static-field")) {
				String fieldName = staticElement.getAttribute("name");
				if (fieldName == null) {
					throw new ConfigException("Missing <name> attribute from static field initialization |%s|.", configSection);
				}
				if (!staticElement.hasAttribute("value")) {
					throw new ConfigException("Missing <value> attribute from static field initialization |%s|.", configSection);
				}

				Field staticField = Classes.getOptionalField(pojoClass, fieldName);
				if (staticField == null) {
					throw new ConfigException("Missing POJO static field |%s#%s|.", pojoClassName, fieldName);
				}
				int modifiers = staticField.getModifiers();
				if (!Modifier.isStatic(modifiers)) {
					throw new ConfigException("Attempt to execute POJO |%s| static initialization on instance field |%s|.", pojoClassName, fieldName);
				}

				Object value = staticElement.getAttribute("value", staticField.getType());
				log.debug("Intialize static field |%s#%s| |%s|", pojoClassName, fieldName, value);
				Classes.setFieldValue(null, staticField, value);
			}
		}
	}
}
