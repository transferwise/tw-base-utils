package com.transferwise.common.baseutils.jdbc;

import javax.sql.DataSource;

public interface ParentAwareDataSourceProxy extends DataSourceProxy {

  DataSource getParentDataSource();

  void setParentDataSource(DataSource dataSource);

}
