package com.transferwise.common.baseutils.jdbc;

import java.sql.Connection;

public interface ParentAwareConnectionProxy extends ConnectionProxy {

  Connection getParentConnection();

  void setParentConnection(Connection connection);
}
