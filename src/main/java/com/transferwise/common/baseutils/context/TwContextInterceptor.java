package com.transferwise.common.baseutils.context;

import java.util.function.Supplier;

public interface TwContextInterceptor {
    boolean applies(TwContext context);

    <T> T intercept(Supplier<T> supplier);
}
