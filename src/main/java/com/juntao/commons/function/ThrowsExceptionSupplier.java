package com.juntao.commons.function;

@FunctionalInterface
public interface ThrowsExceptionSupplier<R> {
	R get() throws Exception;
}
