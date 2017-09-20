package org.totschnig.myexpenses.util;

import android.test.mock.MockContext;

import com.google.android.vending.licensing.PreferenceObfuscator;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import static org.mockito.Mockito.mock;

@RunWith(JUnitParamsRunner.class)
public class LicenceHandlerTest {

  private LicenceHandler licenceHandler;

  @Before
  public void setUp() throws Exception {
    licenceHandler = new LicenceHandler(new MockContext(), mock(PreferenceObfuscator.class));
  }

  @Test
  @Parameters({
      "null, CONTRIB, false",
      "null, EXTENDED, false",
      "null, PROFESSIONAL, false",
      "CONTRIB, CONTRIB, true",
      "CONTRIB, EXTENDED, false",
      "CONTRIB, PROFESSIONAL, false",
      "EXTENDED, CONTRIB, true",
      "EXTENDED, EXTENDED, true",
      "EXTENDED, PROFESSIONAL, false",
      "PROFESSIONAL, CONTRIB, true",
      "PROFESSIONAL, EXTENDED, true",
      "PROFESSIONAL, PROFESSIONAL, true",
  })
  public void isEnabledFor(String hasStatus, String requestedStatus, boolean expected) throws Exception {
    licenceHandler.licenceStatus = parse(hasStatus);
    Assert.assertEquals(expected, licenceHandler.isEnabledFor(LicenceHandler.LicenceStatus.valueOf(requestedStatus)));
  }

  @Test
  @Parameters({
      "CONTRIB, true",
      "EXTENDED, true",
      "PROFESSIONAL, false"
  })
  public void isUpgradeable(String hasStatus, boolean expected) throws Exception {
    licenceHandler.licenceStatus =parse(hasStatus);
    Assert.assertEquals(expected, licenceHandler.isUpgradeable());
  }

  @Test
  @Parameters({
      "CONTRIB, null, true",
      "CONTRIB, CONTRIB, true",
      "CONTRIB, EXTENDED, false",
      "CONTRIB, PROFESSIONAL, false",
      "EXTENDED, null, true",
      "EXTENDED, CONTRIB, true",
      "EXTENDED, EXTENDED, true",
      "EXTENDED, PROFESSIONAL, false",
      "PROFESSIONAL, null, true",
      "PROFESSIONAL, CONTRIB, true",
      "PROFESSIONAL, EXTENDED, true",
      "PROFESSIONAL, PROFESSIONAL, true",
  })
  public void greaterOrEqual(String self, String other, boolean expected) {
    Assert.assertEquals(expected, parse(self).greaterOrEqual(parse(other)));
  }

  private LicenceHandler.LicenceStatus parse(String licenceStatus) {
    try {
      return LicenceHandler.LicenceStatus.valueOf(licenceStatus);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}