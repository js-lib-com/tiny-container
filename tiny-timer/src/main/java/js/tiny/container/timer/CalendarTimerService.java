package js.tiny.container.timer;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Schedule;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.spi.IInstancePostConstructProcessor;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IAnnotationsScanner;
import js.util.Params;

public class CalendarTimerService implements IInstancePostConstructProcessor, IAnnotationsScanner {
	private static final Log log = LogFactory.getLog(CalendarTimerService.class);

	private static final int SCHEDULERS_THREAD_POLL = 2;
	private final ScheduledExecutorService scheduler;

	private final Map<Class<?>, Set<IManagedMethod>> classTimers = new HashMap<>();

	public CalendarTimerService() {
		log.trace("CalendarTimerService()");
		scheduler = Executors.newScheduledThreadPool(SCHEDULERS_THREAD_POLL);
	}

	/** Test constructor. */
	CalendarTimerService(ScheduledExecutorService scheduler) {
		this.scheduler = scheduler;
	}

	@Override
	public Iterable<Annotation> scanClassAnnotations(IManagedClass<?> managedClass) {
		return Collections.emptyList();
	}

	@Override
	public Iterable<Annotation> scanMethodAnnotations(IManagedMethod managedMethod) {
		Schedule schedule = managedMethod.scanAnnotation(Schedule.class);
		if (schedule != null) {
			Set<IManagedMethod> timerMethods = classTimers.get(managedMethod.getDeclaringClass().getImplementationClass());
			if (timerMethods == null) {
				timerMethods = new HashSet<>();
				classTimers.put(managedMethod.getDeclaringClass().getImplementationClass(), timerMethods);
			}
			timerMethods.add(managedMethod);
		}
		return Collections.emptyList();
	}

	@Override
	public Priority getPriority() {
		return Priority.TIMER;
	}

	@Override
	public <T> void onInstancePostConstruct(final T instance) {
		Class<?> implementationClass = instance.getClass();
		Set<IManagedMethod> timerMethods = classTimers.get(implementationClass);
		if (timerMethods == null) {
			return;
		}
		// computed remaining time can be zero in which case managed method is executed instantly
		timerMethods.forEach(managedMethod -> {
			schedule(new TimerTask(this, instance, managedMethod), computeDelay(managedMethod.scanAnnotation(Schedule.class)));
		});
	}

	@Override
	public void destroy() {
		log.trace("destroy()");
		classTimers.clear();
		List<Runnable> timers = scheduler.shutdownNow();
		log.debug("Shutdown %d unprocessed timer(s).", timers.size());
	}

	public void schedule(TimerTask task, Long delay) {
		Params.notNull(task, "Timer task");
		Params.notNull(delay, "Delay");
		scheduler.schedule(task, delay, TimeUnit.MILLISECONDS);
	}

	/**
	 * Return remaining time, in milliseconds, to the next scheduler timeout or 0 if given schedule is passed. This method
	 * creates evaluation date to <code>now</code> and delegates {@link #getNextTimeout(CalendarEx, Schedule)}.
	 * 
	 * @param schedule method configured schedule.
	 * @return delay to the next scheduler timeout, in milliseconds, or zero if no schedule passed.
	 */
	public long computeDelay(Schedule schedule) {
		final Date now = new Date();
		Date next = getNextTimeout(now, schedule);
		if (next == null) {
			// schedule is passed and there are no more future timeouts
			return 0;
		}

		long delay = (next.getTime() - now.getTime());
		log.debug("Next execution date |%s|. Delay is |%d|", next, delay);
		return delay;
	}

	/**
	 * Return next scheduler timeout or null if given schedule instance is passed. This method is designed to be invoked by
	 * {@link #getTimeRemaining(Schedule)} and does alter given <code>now</code> parameter.
	 * 
	 * @param evaluationMoment next timeout evaluation moment,
	 * @param schedule method configured schedule.
	 * @return next scheduler event or null if configured schedule is passed.
	 */
	Date getNextTimeout(Date now, Schedule schedule) {
		CalendarEx evaluationMoment = new CalendarEx(now);
		// next scheduler timeout should be at least one second after the evaluation moment
		evaluationMoment.increment(CalendarUnit.SECOND);
		log.debug("Evaluation moment: %s.", evaluationMoment.getTime());

		// next timeout calendar is started with evaluation moment - that is already incremented with one second
		CalendarEx nextTimeout = evaluationMoment.clone();

		// calendar units are ordered top-down, from years to seconds
		// this is critical because we need to know year and month when parse days
		// remember that days number depends on month and even year

		for (int i = 0; i < CalendarUnit.length(); ++i) {
			CalendarUnit unit = CalendarUnit.get(i);
			IScheduleExpressionParser parser = getCalendarUnitParser(unit);
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
		log.debug("Computed next timeout: %s.", date);
		return date;
	}

	public IScheduleExpressionParser getCalendarUnitParser(CalendarUnit unit) {
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
