package com.juntao.commons.util;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by psw on 2017/2/16.
 */
public class SDateUtil {

	public static final String yyyyMMdd(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		StringBuilder buffer = new StringBuilder();
		buffer.append(calendar.get(Calendar.YEAR));
		leftPad(buffer, calendar.get(Calendar.MONTH) + 1);
		leftPad(buffer, calendar.get(Calendar.DATE));

		return buffer.toString();
	}

	public static final String yyyyMMddHHmmss(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		StringBuilder buffer = new StringBuilder();
		buffer.append(calendar.get(Calendar.YEAR));
		leftPad(buffer, calendar.get(Calendar.MONTH) + 1);
		leftPad(buffer, calendar.get(Calendar.DATE));
		leftPad(buffer, calendar.get(Calendar.HOUR_OF_DAY));
		leftPad(buffer, calendar.get(Calendar.MINUTE));
		leftPad(buffer, calendar.get(Calendar.SECOND));

		return buffer.toString();
	}

	private static void leftPad(StringBuilder buffer, int number) {
		if (number < 10) {
			buffer.append("0");
		}
		buffer.append(number);
	}

	public static void main(String[] args) throws Exception {
		Date date = new Date();

		System.out.println(yyyyMMdd(date));
		System.out.println(yyyyMMddHHmmss(date));
	}

}
