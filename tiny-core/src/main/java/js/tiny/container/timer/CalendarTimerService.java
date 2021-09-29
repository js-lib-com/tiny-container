package js.tiny.container.timer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Schedule;
import javax.inject.Inject;

import js.lang.BugError;
import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.InstanceScope;
import js.tiny.container.spi.IManagedClass;
import js.tiny.container.spi.IManagedMethod;
import js.tiny.container.spi.IServiceMeta;
import js.util.Params;
import js.util.Types;

public class CalendarTimerService implements ICalendarTimerService {
	private static final Log log = LogFactory.getLog(CalendarTimerService.class);

	private static final int SCHEDULERS_THREAD_POLL = 2;
	private final ScheduledExecutorService scheduler;

	@Inject
	public CalendarTimerService() {
		log.trace("CalendarTimerService()");
		scheduler = Executors.newScheduledThreadPool(SCHEDULERS_THREAD_POLL);
	}

	public CalendarTimerService(ScheduledExecutorService scheduler) {
		this.scheduler = scheduler;
	}

	@Override
	public List<IServiceMeta> scan(IManagedClass managedClass) {
		// TODO Auto-generated method stub
		return Collections.emptyList();
	}

	@Override
	public List<IServiceMeta> scan(IManagedMethod managedMethod) {
		List<IServiceMeta> serviceMetas = new ArrayList<>();

		Schedule schedule = managedMethod.getAnnotation(Schedule.class);
		if (schedule != null) {
			if (!managedMethod.getDeclaringClass().getInstanceScope().equals(InstanceScope.APPLICATION)) {
				throw new BugError("Crom method requires %s instance scope.", InstanceScope.APPLICATION);
			}
			if (!Types.isVoid(managedMethod.getReturnType())) {
				throw new BugError("Cron method |%s| must be void.", managedMethod);
			}
			serviceMetas.add(new ScheduleMeta(schedule));
		}

		return serviceMetas;
	}

	@Override
	public void postProcessInstance(IManagedClass managedClass, Object instance) {
		// TODO: core should invoke this hook only for instances with timer service
		// for now core hit this point for every managed instance

		for (IManagedMethod managedMethod : managedClass.getManagedMethods()) {
			ScheduleMeta schedule = managedMethod.getServiceMeta(ScheduleMeta.class);
			if (schedule != null) {
				createTimer(instance, managedMethod);
			}
		}
	}

	@Override
	public void createTimer(Object instance, IManagedMethod managedMethod) {
		log.debug("Create timer for method |%s|.", managedMethod);

		ScheduleMeta schedule = managedMethod.getServiceMeta(ScheduleMeta.class);
		Params.notNull(schedule, "Managed method schedule");

		// computed remaining time can be zero in which case managed method is executed instantly
		schedule(new TimerTask(this, instance, managedMethod), computeDelay(schedule));
	}

	@Override
	public void destroy() {
		log.trace("destroy()");
		List<Runnable> timers = scheduler.shutdownNow();
		log.debug("Shutdown %d unprocessed timer(s).", timers.size());
	}

	public void schedule(TimerTask task, long delay) {
		scheduler.schedule(task, delay, TimeUnit.MILLISECONDS);
	}

	/**
	 * Return remaining time, in milliseconds, to the next scheduler timeout or 0 if given schedule is passed. This method
	 * creates evaluation date to <code>now</code> and delegates {@link #getNextTimeout(CalendarEx, Schedule)}.
	 * 
	 * @param schedule method configured schedule.
	 * @return delay to the next scheduler timeout, in milliseconds, or zero if no schedule passed.
	 */
	public long computeDelay(ScheduleMeta schedule) {
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
	Date getNextTimeout(Date now, ScheduleMeta schedule) {
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
