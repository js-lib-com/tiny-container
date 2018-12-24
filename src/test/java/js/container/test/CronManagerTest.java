package js.container.test;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;

import org.junit.Test;

import js.container.CronManager;
import js.util.Classes;

public class CronManagerTest {
	@Test
	public void computeDelay() {
		Calendar calendar = Calendar.getInstance();

		assertEquals(5, delay(String.format("%d %d * * *", calendar.get(Calendar.MINUTE) + 5, calendar.get(Calendar.HOUR_OF_DAY))));
		assertEquals(1440, delay(String.format("%d %d * * *", calendar.get(Calendar.MINUTE), calendar.get(Calendar.HOUR_OF_DAY))));
		
		assertEquals(5, delay(String.format("%d %d %d * *", calendar.get(Calendar.MINUTE) + 5, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.DATE))));
		assertEquals(44640, delay(String.format("%d %d %d * *", calendar.get(Calendar.MINUTE), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.DATE))));
	}

	private static long delay(String cronExpression) {
		System.out.println(cronExpression);
		try {
			return Classes.invoke(CronManager.class, "delay", cronExpression);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
}
