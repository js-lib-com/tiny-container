package js.container;

import js.log.Log;
import js.log.LogFactory;

/**
 * Dump managed instance to {@link Container} class logger. This processor dumps instance managed class to container logger but
 * only if scope is {@link InstanceScope#APPLICATION} and type {@link InstanceType#PROXY}. This processor does not update
 * instance and does not throw any exception.
 * <p>
 * This processor is for debugging and works on behalf of container, that is, uses container class logger to write dump record.
 * 
 * @author Iulian Rotaru
 * @version final
 */
final class LoggerInstanceProcessor implements InstanceProcessor {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(LoggerInstanceProcessor.class);

	/**
	 * Dump managed instance of {@link InstanceType#PROXY} type and with {@link InstanceScope#APPLICATION} scope to container
	 * logger. For managed instance of other scope / type combinations this method does nothing.
	 * 
	 * @param managedClass managed class,
	 * @param instance instance of managed class.
	 */
	@Override
	public void postProcessInstance(ManagedClassSPI managedClass, Object instance) {
		if (!managedClass.getInstanceScope().equals(InstanceScope.APPLICATION)) {
			return;
		}
		if (!managedClass.getInstanceType().equals(InstanceType.PROXY)) {
			return;
		}

		Class<?>[] interfaceClasses = managedClass.getInterfaceClasses();
		StringBuilder interfaceNames = new StringBuilder(interfaceClasses[0].getName());
		for (int i = 1; i < interfaceClasses.length; ++i) {
			interfaceNames.append(", ");
			interfaceNames.append(interfaceClasses[i].getName());
		}

		log.debug("Create managed container proxy:\r\n" + //
				"\t- implementation: %s\r\n" + //
				"\t- interface(s): %s\r\n" + //
				"\t- scope: %s\r\n" + //
				"\t- type: %s\r\n" + //
				"\t- transactional: %s\r\n" + //
				"\t- remote: %s", managedClass.getImplementationClass(), interfaceNames.toString(), managedClass.getInstanceScope(), managedClass.getInstanceType(), managedClass.isTransactional(), managedClass.isRemotelyAccessible());
	}
}
