package com.transferwise.common.baseutils.jdbc;

import javax.sql.DataSource;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DataSourceProxyUtils {

  public static void tieTogether(DataSource parent, DataSource child) {
    if (parent instanceof DataSourceProxy) {
      ((DataSourceProxy) parent).setTargetDataSource(child);
    }

    if (child instanceof ParentAwareDataSourceProxy) {
      ((ParentAwareDataSourceProxy) child).setParentDataSource(parent);
    }
  }
}
