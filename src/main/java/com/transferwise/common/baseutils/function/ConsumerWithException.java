package com.transferwise.common.baseutils.function;

@FunctionalInterface
public interface ConsumerWithException<T> {
    @SuppressWarnings("RedundantThrows")
    void accept(T t) throws Exception;
}
