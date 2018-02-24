package com.juntao.commons.function;

@FunctionalInterface
public interface ThrowsExceptionFunction<T, R> {
	R apply(T t) throws Exception;
}