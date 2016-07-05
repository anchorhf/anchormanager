/**
 * 
 */
/**
 * @author Administrator
 *
 */
package com.anke.util;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class TimerManager {

	/**
	 * @param args
	 */

	// 时间间隔()
	private static long PERIOD_DAY = 24 * 60 * 60 * 1000;

	public TimerManager(TimerTask task, long period_Time,long ExecuteSynchronization,int synchronizationHour,int synchronizationMinute,int synchronizationSecond) {
		Calendar calendar = Calendar.getInstance();

		Date date = calendar.getTime(); // 第一次执行定时任务的时间
		
        if(ExecuteSynchronization==0){
		calendar.set(Calendar.HOUR_OF_DAY, synchronizationHour); // 凌晨1点
		calendar.set(Calendar.MINUTE, synchronizationMinute);
		calendar.set(Calendar.SECOND, synchronizationSecond);
		date = calendar.getTime(); // 第一次执行定时任务的时间
		// 如果第一次执行定时任务的时间 小于当前的时间
		// 此时要在 第一次执行定时任务的时间加一天，以便此任务在下个时间点执行。如果不加一天，任务会立即执行。
    		if (date.before(new Date())) {
    			date = this.addDay(date, 1);
    		}
        }

		Timer timer = new Timer();
		// NFDFlightDataTimerTask task = new NFDFlightDataTimerTask();
		if (period_Time >= 0) {
			PERIOD_DAY = period_Time * 60 * 60 * 1000;
			//PERIOD_DAY = period_Time * 60 * 1000;//测试1分钟执行一次
		}
		// 安排指定的任务在指定的时间开始进行重复的固定延迟执行。
		timer.schedule(task, date, PERIOD_DAY);
	}

	// 增加或减少天数
	public Date addDay(Date date, int num) {
		Calendar startDT = Calendar.getInstance();
		startDT.setTime(date);
		startDT.add(Calendar.DAY_OF_MONTH, num);
		return startDT.getTime();
	}

}