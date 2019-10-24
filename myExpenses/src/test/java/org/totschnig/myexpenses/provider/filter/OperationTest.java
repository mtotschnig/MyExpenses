package org.totschnig.myexpenses.provider.filter;

import junit.framework.Assert;
import junit.framework.TestCase;

public class OperationTest extends TestCase {

  public void testGetOp() {
    Assert.assertEquals("IN ()", WhereFilter.Operation.IN.getOp(0));
    Assert.assertEquals("IN (?)", WhereFilter.Operation.IN.getOp(1));
    Assert.assertEquals("IN (?,?)", WhereFilter.Operation.IN.getOp(2));
    Assert.assertEquals("IN (?,?,?)", WhereFilter.Operation.IN.getOp(3));

  }
}