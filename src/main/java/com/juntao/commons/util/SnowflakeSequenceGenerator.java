package com.juntao.commons.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

public final class SnowflakeSequenceGenerator {
	private static long sequence = 0l;

	private static long lastTimestamp = -1l;

	private static final ReentrantLock lock = new ReentrantLock();

	private static final long workIdBits = 10l;

	private static final long sequenceBits = 12l;

	private static final long workIdLeftShift = sequenceBits;

	private static final long timeStampLeftShift = workIdBits + sequenceBits;

	private static final long sequenceMask = -1 ^ (-1 << sequenceBits);

	private static final long genesis = 1429113600000l;// 2015-04-16 00:00:00
														// GMT+0800

	private final short workId;

	@Deprecated
	private SnowflakeSequenceGenerator(){
		this.workId = Short.MIN_VALUE;
	}

	public SnowflakeSequenceGenerator(short workId) {
		this.workId = workId;
	}

	public final long nextId() {
		long nexdId = -1l;

		lock.lock();
		try {
			long timeStamp = timeGen();
			if (lastTimestamp == timeStamp) {
				sequence = (sequence + 1) & sequenceMask;
				if (sequence == 0l) {
					timeStamp = tillNextMills(lastTimestamp);
				}
			} else {
				sequence = 0l;
			}

			if (timeStamp < lastTimestamp) {
				throw new RuntimeException("system clock fall back");
			}

			lastTimestamp = timeStamp;

			nexdId = ((timeStamp - genesis) << timeStampLeftShift) | (workId << workIdLeftShift) | (sequence);

		} finally {
			lock.unlock();
		}
		return nexdId;
	}

	private static long timeGen() {
		return System.currentTimeMillis();
	}

	private static long tillNextMills(long lastTimestamp) {
		long timeStamp = timeGen();
		while (timeStamp <= lastTimestamp) {
			timeStamp = timeGen();
		}
		return timeStamp;
	}

	public static class SequenceDecoder {
		private static long timeStampMask = -1l ^ (-1l << 41);

		private static long workIdMask = -1l ^ (-1l << workIdBits);

		private static long sequenceMask = -1l ^ (-1l << sequenceBits);

		private static long workIdRightShift = sequenceBits;

		private static long timeStampRightShift = workIdBits + sequenceBits;

		public static long sequence(long l) {
			return l & sequenceMask;
		}

		public static long workId(long l) {
			return (l >> workIdRightShift) & workIdMask;
		}

		public static long timeStamp(long l) {
			return (l >> timeStampRightShift) & timeStampMask;
		}

		public static String formattedDateString(long l) {
			long timeStamp = timeStamp(l);
			long real = timeStamp + genesis;
			SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmssS");
			return fmt.format(new Date(real));
		}
	}

}
