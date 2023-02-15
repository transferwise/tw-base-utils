package com.transferwise.common.baseutils.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DataSourceProxyUtilsTest {

  @Test
  void testThatDataSourcesCanBeTied() {
    var parent = new MockDataSource();
    var child = new MockDataSource();

    DataSourceProxyUtils.tieTogether(parent, child);

    assertEquals(parent.getTargetDataSource(), child);
    assertEquals(child.getParentDataSource(), parent);
  }
}
