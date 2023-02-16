package com.transferwise.common.baseutils.function;

@FunctionalInterface
public interface FunctionWithException<T, R> {

  @SuppressWarnings("RedundantThrows")
  R apply(T t) throws Exception;
}
