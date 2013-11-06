package org.totschnig.myexpenses.test.misc;

import org.totschnig.myexpenses.MyApplication;

import junit.framework.Assert;
import junit.framework.TestCase;

public class SafeGuardTests extends TestCase {
  public void testDebugIsFalse() {
    Assert.assertFalse(MyApplication.debug);
  }
}
