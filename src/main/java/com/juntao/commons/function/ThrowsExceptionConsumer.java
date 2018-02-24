package com.juntao.commons.function;

@FunctionalInterface
public interface ThrowsExceptionConsumer<T> {
	void accept(T t) throws Exception;
}
