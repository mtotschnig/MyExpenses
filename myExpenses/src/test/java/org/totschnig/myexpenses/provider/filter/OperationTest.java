package org.totschnig.myexpenses.provider.filter;

import junit.framework.Assert;
import junit.framework.TestCase;

public class OperationTest extends TestCase {

  public void testGetOp() throws Exception {
    Assert.assertEquals("IN (?)",WhereFilter.Operation.IN.getOp(1));
    Assert.assertEquals("IN (?,?)",WhereFilter.Operation.IN.getOp(2));
    Assert.assertEquals("IN (?,?,?)",WhereFilter.Operation.IN.getOp(3));

  }
}