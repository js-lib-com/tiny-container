package com.jslib.container.timer;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.container.spi.IInstancePostConstructProcessor;
import com.jslib.container.spi.IManagedClass;
import com.jslib.container.spi.ServiceConfigurationException;
import com.jslib.util.Params;
import com.jslib.util.Types;

import jakarta.ejb.Schedule;

/**
 * Calendar based timer service based on {@link Schedule} annotation. This class scans for timer methods, see
 * {@link #bind(IManagedClass)} and register found timers to {@link #classTimers}. When an instance of a managed class that has
 * timer methods is created - see {@link #onInstancePostConstruct(Object)}, takes care to schedule timer tasks. On timer task
 * execution complete re-schedule the timer task to the next execution time from calendar.
 * 
 * @author Iulian Rotaru
 */
public class CalendarTimerService implements IInstancePostConstructProcessor {
	private static final Log log = LogFactory.getLog(CalendarTimerService.class);

	private static final long TIMER_TERMINATE_TIMEOUT = 4000;

	private static final int SCHEDULERS_THREAD_POLL = 2;
	private final ScheduledExecutorService scheduler;

	private final Map<Class<?>, Set<Method>> classTimers = new HashMap<>();
	private final Map<Method, Schedule> methodSchedules = new HashMap<>();

	public CalendarTimerService() {
		scheduler = Executors.newScheduledThreadPool(SCHEDULERS_THREAD_POLL, new TimerTaskFactory());
	}

	/** Test constructor. */
	CalendarTimerService(ScheduledExecutorService scheduler) {
		this.scheduler = scheduler;
	}

	@Override
	public Priority getPriority() {
		return Priority.TIMER;
	}

	/**
	 * This method scans managed class for {@link Schedule} annotation and register found timer methods, if any. Both managed
	 * class interface and implementation are scanned. Ignore annotation if timer method is redeclared on implementation class.
	 * 
	 * All methods are scanned, including private methods. Anyway, candidate timer method is subject to validation - see
	 * {@link CalendarTimerService#sanityCheck(Method)}.
	 * 
	 * Returns true if at least one timer method was found, to signal container that given managed class parameter has a
	 * service.
	 * 
	 * @param managedClass managed class to scan for {@link Schedule} annotation.
	 * @return true only if at least one timer method has found.
	 * @throws ServiceConfigurationException if {@link Schedule} annotation is used on a not valid method.
	 */
	@Override
	public <T> boolean bind(IManagedClass<T> managedClass) {
		final Set<Method> timers = new HashSet<>();
		final List<String> names = new ArrayList<>();

		Consumer<Class<?>> scanner = clazz -> {
			for (Method method : clazz.getDeclaredMethods()) {
				Schedule schedule = method.getAnnotation(Schedule.class);
				if (schedule != null) {
					method.setAccessible(true);
					sanityCheck(method);

					if (names.contains(method.getName())) {
						log.warn("Timer method {} redeclared on managed class implementation. Ignore annotation.", method);
						continue;
					}
					names.add(method.getName());

					timers.add(method);
					methodSchedules.put(method, schedule);
				}
			}
		};

		scanner.accept(managedClass.getInterfaceClass());
		scanner.accept(managedClass.getImplementationClass());

		if (timers.isEmpty()) {
			return false;
		}

		classTimers.put(managedClass.getImplementationClass(), timers);
		return true;
	}

	/**
	 * Check if method qualifies as timer method. This method throws service configuration exception if method:
	 * <ul>
	 * <li>is static or final,
	 * <li>has formal parameters,
	 * <li>does returns value,
	 * <li>does throw checked exception.
	 * </ul>
	 * 
	 * @param method method to validate.
	 * @throws ServiceConfigurationException if given method does not qualifies as timer method.
	 */
	private static void sanityCheck(Method method) {
		if (Modifier.isStatic(method.getModifiers())) {
			throw new ServiceConfigurationException("Timer callback %s must not be static.", method);
		}
		if (Modifier.isFinal(method.getModifiers())) {
			throw new ServiceConfigurationException("Timer callback %s must not be final.", method);
		}
		if (!Types.isVoid(method.getReturnType())) {
			throw new ServiceConfigurationException("Timer callback %s must be void.", method);
		}
		if (method.getExceptionTypes().length > 0) {
			throw new ServiceConfigurationException("Timer callback %s must not throw checked exceptions.", method);
		}
	}

	/**
	 * This handler is invoked after managed instance creation and takes care to create and schedule timer task(s). This method
	 * assume that managed instance parameter is a managed class with at least one timer method, as detected by
	 * {@link #bind(IManagedClass)} method - if bind returns false it is expected that container does not invoke this handler
	 * for that particular managed class.
	 * 
	 * @param instance managed instance.
	 */
	@Override
	public <T> void onInstancePostConstruct(final T instance) {
		log.trace("onInstancePostConstruct(final T)");

		Class<?> implementationClass = instance.getClass();
		Set<Method> timerMethods = classTimers.get(implementationClass);
		assert timerMethods != null;

		// computed remaining time can be zero in which case managed method is executed instantly
		timerMethods.forEach(managedMethod -> {
			schedule(new TimerTask(this, instance, managedMethod), computeDelay(managedMethod));
		});
	}

	// TODO: use onInstancePreDestroy to purge instance related timer(s)

	@Override
	public synchronized void destroy() {
		log.trace("destroy()");
		classTimers.clear();
		try {
			log.debug("Initiate graceful scheduler shutdown.");
			scheduler.shutdown();
			if (!scheduler.awaitTermination(TIMER_TERMINATE_TIMEOUT, TimeUnit.MILLISECONDS)) {
				log.warn("Timeout waiting for timer termination. Force scheduler shutdown now.");
				scheduler.shutdownNow();
			}
		} catch (InterruptedException e) {
			log.error(e);
		}
	}

	// --------------------------------------------------------------------------------------------

	synchronized void schedule(TimerTask task, long delay) {
		Params.notNull(task, "Timer task");
		scheduler.schedule(task, delay, TimeUnit.MILLISECONDS);
	}

	/**
	 * Return remaining time, in milliseconds, to the next scheduler timeout or 0 if given schedule is passed. This method
	 * creates evaluation date to <code>now</code> and delegates {@link #getNextTimeout(Date, Schedule)}.
	 * 
	 * @param schedule method configured schedule.
	 * @return delay to the next scheduler timeout, in milliseconds, or zero if no schedule passed.
	 */
	long computeDelay(Method managedMethod) {
		Schedule schedule = methodSchedules.get(managedMethod);
		final Date now = new Date();
		Date next = getNextTimeout(now, schedule);
		if (next == null) {
			// schedule is passed and there are no more future timeouts
			return 0;
		}

		long delay = next.getTime() - now.getTime();
		log.debug("Next execution date |{schedule_date}|. Delay is |{schedule_delay}|", next, delay);
		return delay;
	}

	/**
	 * Return next scheduler timeout or null if given schedule instance is passed. This method is designed to be invoked by
	 * {@link #computeDelay(Method)} and does alter given <code>now</code> parameter.
	 * 
	 * @param evaluationMoment next timeout evaluation moment,
	 * @param schedule method configured schedule.
	 * @return next scheduler event or null if configured schedule is passed.
	 */
	Date getNextTimeout(Date now, Schedule schedule) {
		CalendarEx evaluationMoment = new CalendarEx(now);
		// next scheduler timeout should be at least one second after the evaluation moment
		evaluationMoment.increment(CalendarUnit.SECOND);

		// next timeout calendar is started with evaluation moment - that is already incremented with one second
		CalendarEx nextTimeout = evaluationMoment.clone();

		// calendar units are ordered top-down, from years to seconds
		// this is critical because we need to know year and month when parse days
		// remember that days number depends on month and even year

		for (int i = 0; i < CalendarUnit.length(); ++i) {
			CalendarUnit unit = CalendarUnit.get(i);
			IScheduleExpressionParser parser = getScheduleExpressionParser(unit);
			parser.parse(schedule, nextTimeout);

			if (nextTimeout.after(evaluationMoment) && !nextTimeout.isUpdated(unit)) {
				// reset to minimal acceptable value for specific unit but only if unit was not already set
				// keep in mind that this loop can be executed multiple times because of unit value overflow
				nextTimeout.set(unit, parser.getMinimumValue());
				continue;
			}

			int currentValue = nextTimeout.get(unit);
			NextValue nextValue = parser.getNextValue(currentValue);
			if (nextValue == null) {
				return null;
			}

			if (nextValue.isOverflow()) {
				// increment parent unit value, reset the current overflowing unit and restart evaluation loop back from years
				nextTimeout.increment(unit.getParentUnit());
				nextTimeout.set(unit, nextTimeout.getActualMinimum(unit));
				i = -1;
				continue;
			}

			nextTimeout.set(unit, nextValue.getValue());
		}

		Date date = nextTimeout.getTime();
		return date;
	}

	IScheduleExpressionParser getScheduleExpressionParser(CalendarUnit unit) {
		switch (unit) {
		case SECOND:
		case MINUTE:
		case HOUR:
			return new NumericExpressionParser(unit);

		case DAY:
			return new DayExpressionParser();

		case MONTH:
			return new MonthExpressionParser();

		case YEAR:
			return new YearExpressionParser();

		default:
			throw new IllegalStateException();
		}
	}
}
