package com.transferwise.common.baseutils.function;

@FunctionalInterface
public interface FunctionWithException<T, R> {
	R apply(T t) throws Exception;
}
