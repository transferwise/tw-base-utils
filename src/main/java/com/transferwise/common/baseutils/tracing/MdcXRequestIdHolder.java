package com.transferwise.common.baseutils.tracing;

import com.transferwise.common.baseutils.ExceptionUtils;
import com.transferwise.common.baseutils.function.RunnableWithException;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.concurrent.Callable;

public class MdcXRequestIdHolder implements IXRequestIdHolder {

    @Setter
    private String mdcKey;

    public MdcXRequestIdHolder() {
        this("xRequestId");
    }

    public MdcXRequestIdHolder(String mdcKey) {
        this.mdcKey = mdcKey;
    }

    @Override
    public String generate() {
        return UUID.randomUUID().toString();
    }

    @Override
    public String current() {
        return MDC.get(mdcKey);
    }

    @Override
    public void set(String requestId) {
        if (requestId == null) {
            MDC.remove(mdcKey);
        } else {
            MDC.put(mdcKey, requestId);
        }
    }

    @Override
    public void remove(String requestId) {
        MDC.remove(mdcKey);
    }

    @Override
    public <T> T with(Object o, Callable<T> callable) {
        return ExceptionUtils.doUnchecked(() -> {
            String currentXRequestId = current();
            try {
                String xRequestId = currentXRequestId;
                if (o instanceof IWithXRequestId) {
                    xRequestId = ((IWithXRequestId) o).getXRequestId();
                }
                if (StringUtils.isEmpty(xRequestId)) {
                    xRequestId = generate();
                }
                set(xRequestId);
                return callable.call();
            } finally {
                set(currentXRequestId);
            }
        });
    }

    @Override
    public void with(Object o, RunnableWithException runnable) {
        with(o, () -> {
            runnable.run();
            return null;
        });
    }
}
