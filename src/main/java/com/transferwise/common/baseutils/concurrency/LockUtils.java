package com.transferwise.common.baseutils.concurrency;

import lombok.experimental.UtilityClass;

import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

@UtilityClass
public class LockUtils {
    public static void withLock(Lock lock, Runnable runnable) {
        lock.lock();
        try {
            runnable.run();
        } finally {
            lock.unlock();
        }
    }

    public static <T> T withLock(Lock lock, Supplier<T> supplier) {
        lock.lock();
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

}
