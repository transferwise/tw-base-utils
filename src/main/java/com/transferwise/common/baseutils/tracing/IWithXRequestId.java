package com.transferwise.common.baseutils.tracing;

public interface IWithXRequestId {
    void setXRequestId(String xRequestId);

    String getXRequestId();
}
