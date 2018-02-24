package com.juntao.commons.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SLockAndFuncUtil {
	private static final Logger log = LoggerFactory.getLogger(SLockAndFuncUtil.class);

	private static final boolean tryLock(Lock lock, long millis) {
		try {
			return lock.tryLock(millis, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			log.error("try lock but interrupted!!", e);
			return false;
		}
	}

	public static final <T, R> R lockAndApply(Lock lock, long millis, Function<T, R> function, T t) {
		if (!tryLock(lock, millis)) {
			return null;
		}

		try {
			return function.apply(t);
		} finally {
			lock.unlock();
		}
	}

	public static final <T> void lockAndAccept(Lock lock, long millis, Consumer<T> consumer, T t) {
		if (!tryLock(lock, millis)) {
			return;
		}

		try {
			consumer.accept(t);
		} finally {
			lock.unlock();
		}
	}

	public static final <R> R lockAndGet(Lock lock, long millis, Supplier<R> supplier) {
		if (!tryLock(lock, millis)) {
			return null;
		}

		try {
			return supplier.get();
		} finally {
			lock.unlock();
		}
	}
}
