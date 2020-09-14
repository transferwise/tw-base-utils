package com.transferwise.common.baseutils.tracing;

/**
 * Old Tw tracking system.
 * 
 * @deprecated in favor of Jaegar tracing.
 */
@Deprecated
public interface IWithXRequestId {

  void setXRequestId(String xRequestId);

  String getXRequestId();
}
