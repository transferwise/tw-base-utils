package com.transferwise.common.baseutils.tracing;

import static java.lang.Character.isLetterOrDigit;

import com.transferwise.common.baseutils.function.RunnableWithException;
import java.util.concurrent.Callable;

/**
 * Old Tw tracking system.
 *
 * @deprecated in favor of Jaegar tracing.
 */
@Deprecated
public interface IXRequestIdHolder {

  int MAX_REQUEST_ID_LENGTH = 36;

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
    if (requestId == null || requestId.length() > MAX_REQUEST_ID_LENGTH) {
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
