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

	// ʱ����()
	private static long PERIOD_DAY = 24 * 60 * 60 * 1000;

	public TimerManager(TimerTask task, long period_Time,long ExecuteSynchronization,int synchronizationHour,int synchronizationMinute,int synchronizationSecond) {
		Calendar calendar = Calendar.getInstance();

		Date date = calendar.getTime(); // ��һ��ִ�ж�ʱ�����ʱ��
		
        if(ExecuteSynchronization==0){
		calendar.set(Calendar.HOUR_OF_DAY, synchronizationHour); // �賿1��
		calendar.set(Calendar.MINUTE, synchronizationMinute);
		calendar.set(Calendar.SECOND, synchronizationSecond);
		date = calendar.getTime(); // ��һ��ִ�ж�ʱ�����ʱ��
		// �����һ��ִ�ж�ʱ�����ʱ�� С�ڵ�ǰ��ʱ��
		// ��ʱҪ�� ��һ��ִ�ж�ʱ�����ʱ���һ�죬�Ա���������¸�ʱ���ִ�С��������һ�죬���������ִ�С�
    		if (date.before(new Date())) {
    			date = this.addDay(date, 1);
    		}
        }

		Timer timer = new Timer();
		// NFDFlightDataTimerTask task = new NFDFlightDataTimerTask();
		if (period_Time >= 0) {
			PERIOD_DAY = period_Time * 60 * 60 * 1000;
			//PERIOD_DAY = period_Time * 60 * 1000;//����1����ִ��һ��
		}
		// ����ָ����������ָ����ʱ�俪ʼ�����ظ��Ĺ̶��ӳ�ִ�С�
		timer.schedule(task, date, PERIOD_DAY);
	}

	// ���ӻ��������
	public Date addDay(Date date, int num) {
		Calendar startDT = Calendar.getInstance();
		startDT.setTime(date);
		startDT.add(Calendar.DAY_OF_MONTH, num);
		return startDT.getTime();
	}

}