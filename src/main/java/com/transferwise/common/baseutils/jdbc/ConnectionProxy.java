package com.transferwise.common.baseutils.jdbc;

import java.sql.Connection;

public interface ConnectionProxy extends Connection {

  Connection getTargetConnection();
}
