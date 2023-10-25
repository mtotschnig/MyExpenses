package org.totschnig.myexpenses.test.misc;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.util.licence.LicenceHandler;

public class SafeGuardTests extends TestCase {
  public void testContribIsNotEnabled() {
    final LicenceHandler licenceHandler = MyApplication.Companion.getInstance().licenceHandler;
    Assert.assertFalse(licenceHandler.isContribEnabled());
    Assert.assertNull(licenceHandler.getLicenceStatus());
  }
}
