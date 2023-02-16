package com.transferwise.common.baseutils.jdbc;

import java.sql.Connection;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ConnectionProxyUtils {

  public static void tieTogether(Connection parent, Connection child) {
    if (parent instanceof ConnectionProxy) {
      ((ConnectionProxy) parent).setTargetConnection(child);
    }

    if (child instanceof ParentAwareConnectionProxy) {
      ((ParentAwareConnectionProxy) child).setParentConnection(parent);
    }
  }
}
