package com.juntao.commons.function;

@FunctionalInterface
public interface ThrowsExceptionTriConsumer<X, Y, Z> {
	void accept(X x, Y y, Z z) throws Exception;
}
