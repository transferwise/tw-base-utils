package com.transferwise.common.baseutils.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConnectionProxyUtilsTest {

  @Test
  void testThatConnectionsCanBeTied() {
    var parent = new MockConnectionProxy();
    var child = new MockConnectionProxy();

    ConnectionProxyUtils.tieTogether(parent, child);

    assertEquals(parent.getTargetConnection(), child);
    assertEquals(child.getParentConnection(), parent);
  }
}
