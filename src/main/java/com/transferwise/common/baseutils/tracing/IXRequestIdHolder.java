package com.transferwise.common.baseutils.tracing;

import com.transferwise.common.baseutils.function.RunnableWithException;

import java.util.concurrent.Callable;

import static java.lang.Character.isLetterOrDigit;

public interface IXRequestIdHolder {

    String generate();

    String current();

    void set(String requestId);

    default boolean isValid(String requestId) {
        return isValidRequestId(requestId);
    }

    void remove(String requestId);

    <T> T with(Object o, Callable<T> callable);

    void with(Object o, RunnableWithException runnable);

    static boolean isValidRequestId(String requestId) {
        if (requestId == null || requestId.length() > 36) {
            return false;
        }

        final int sz = requestId.length();
        for (int i = 0; i < sz; i++) {
            if (!isLetterOrDigit(requestId.charAt(i)) && requestId.charAt(i) != '-') {
                return false;
            }
        }
        return true;
    }
}
