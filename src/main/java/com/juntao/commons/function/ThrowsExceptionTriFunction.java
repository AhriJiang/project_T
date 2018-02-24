package com.juntao.commons.function;

@FunctionalInterface
public interface ThrowsExceptionTriFunction<X, Y, Z, R> {
	R apply(X x, Y y, Z z) throws Exception;
}