package com.transferwise.common.baseutils.tracing;

import static org.junit.Assert.assertEquals;

import com.transferwise.common.baseutils.BaseTest;
import org.junit.jupiter.api.Test;


public class MdcXRequestIdHolderTest extends BaseTest {

  private final MdcXRequestIdHolder mdcXRequestIdHolder = new MdcXRequestIdHolder();

  @Test
  public void testValidity() {
    testValidity(null, false);
    testValidity("?????", false);
    testValidity("a5afaeea-e4df-46ec-bd01-15f66cc2fa93?", false);
    testValidity("a5afaeea-e4df-46ec-bd01-15f66cc2fa93", true);
  }

  private void testValidity(String requestId, boolean isValid) {
    assertEquals(mdcXRequestIdHolder.isValid(requestId), isValid);
  }
}
