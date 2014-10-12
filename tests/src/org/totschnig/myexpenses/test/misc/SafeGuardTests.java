package org.totschnig.myexpenses.test.misc;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.service.PlanExecutor;

import junit.framework.Assert;
import junit.framework.TestCase;

public class SafeGuardTests extends TestCase {
  public void testContribIsNotEnabled() {
    Assert.assertFalse(MyApplication.getInstance().isContribEnabled());
    
  }
}
