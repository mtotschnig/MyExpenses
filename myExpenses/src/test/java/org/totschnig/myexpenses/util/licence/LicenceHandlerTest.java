package org.totschnig.myexpenses.util.licence;

import com.google.android.vending.licensing.PreferenceObfuscator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.util.Objects;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@RunWith(JUnitParamsRunner.class)
public class LicenceHandlerTest {

  private LicenceHandler licenceHandler;

  @Before
  public void setUp() {
    licenceHandler = new LicenceHandler(mock(MyApplication.class), mock(PreferenceObfuscator.class), mock(CrashHandler.class), mock(PrefHandler.class));
  }

  @After
  public void tearDown() {
    licenceHandler.setLicenceStatus(null);
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
  public void isEnabledFor(String hasStatus, String requestedStatus, boolean expected) {
    licenceHandler.setLicenceStatus(parse(hasStatus));
    assertEquals(expected, licenceHandler.isEnabledFor(LicenceStatus.valueOf(requestedStatus)));
  }

  @Test
  @Parameters({
      "CONTRIB, true",
      "EXTENDED, true",
      "PROFESSIONAL, false"
  })
  public void isUpgradeable(String hasStatus, boolean expected) {
    licenceHandler.setLicenceStatus(parse(hasStatus));
    assertEquals(expected, licenceHandler.isUpgradeable());
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
    assertEquals(expected, Objects.requireNonNull(parse(self)).greaterOrEqual(parse(other)));
  }

  private LicenceStatus parse(String licenceStatus) {
    try {
      return LicenceStatus.valueOf(licenceStatus);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}