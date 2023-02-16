package com.transferwise.common.baseutils.jdbc;

import javax.sql.DataSource;

public interface DataSourceProxy extends DataSource {

  DataSource getTargetDataSource();

  void setTargetDataSource(DataSource dataSource);

}
