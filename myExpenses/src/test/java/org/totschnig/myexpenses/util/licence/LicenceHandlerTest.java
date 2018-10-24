package org.totschnig.myexpenses.util.licence;

import com.google.android.vending.licensing.PreferenceObfuscator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.totschnig.myexpenses.MyApplication;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.mock;

@RunWith(JUnitParamsRunner.class)
public class LicenceHandlerTest {

  private LicenceHandler licenceHandler;

  @Before
  public void setUp() throws Exception {
    licenceHandler = new LicenceHandler(mock(MyApplication.class), mock(PreferenceObfuscator.class));
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
    assertEquals(expected, licenceHandler.isEnabledFor(LicenceStatus.valueOf(requestedStatus)));
  }

  @Test
  @Parameters({
      "CONTRIB, true",
      "EXTENDED, true",
      "PROFESSIONAL, false"
  })
  public void isUpgradeable(String hasStatus, boolean expected) throws Exception {
    licenceHandler.licenceStatus =parse(hasStatus);
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
    assertEquals(expected, parse(self).greaterOrEqual(parse(other)));
  }

  @Test
  @Parameters({
      "sku_premium, CONTRIB",
      "sku_extended, EXTENDED",
      "sku_premium2extended, EXTENDED",
      "sku_professional, PROFESSIONAL",
      "sku_professional_monthly, PROFESSIONAL",
      "sku_professional_yearly, PROFESSIONAL",
      "sku_extended2professional, PROFESSIONAL",
      "sku_extended2professional_monthly, PROFESSIONAL",
      "sku_extended2professional_yearly, PROFESSIONAL"
  })
  public void extractLicenceStatusFromSku(String sku, String licenceStatus) {
    LicenceStatus expected = parse(licenceStatus);
    LicenceStatus actual = licenceHandler.extractLicenceStatusFromSku(sku);
    assertNotNull(expected);
    assertNotNull(actual);
    assertEquals(expected, actual);
  }

  @Test
  @Parameters({
      "sku_premium",
      "sku_extended",
      "sku_premium2extended",
      "sku_professional",
      "sku_professional_monthly",
      "sku_professional_yearly",
      "sku_extended2professional",
      "sku_extended2professional_monthly",
      "sku_extended2professional_yearly"
  })
  public void singleValidSkuIsHighest(String provided) {
    assertEquals(provided, licenceHandler.findHighestValidSku(Collections.singletonList(provided)));
  }

  @Test
  public void invalidSkusAreIgnored() {
    assertNull(licenceHandler.findHighestValidSku(Collections.singletonList("bogus")));
    assertNull(licenceHandler.findHighestValidSku(Arrays.asList("bogus1", "bogus2")));
    assertEquals("sku_premium",licenceHandler.findHighestValidSku(Arrays.asList("bogus1", "bogus2", "sku_premium")));
  }

  @Test
  @Parameters
  public void upgradesAreIdentified(List<String> inventory, String sku) {
    assertEquals(sku, licenceHandler.findHighestValidSku(inventory));
  }
  private Object[] parametersForUpgradesAreIdentified() {
    return new Object[]{
        new Object[]{Arrays.asList("sku_premium", "sku_premium2extended"), "sku_premium2extended"},
        new Object[]{Arrays.asList("sku_premium2extended", "sku_premium"), "sku_premium2extended"},

        new Object[]{Arrays.asList("sku_premium", "sku_premium2extended", "sku_extended2professional_monthly"), "sku_extended2professional_monthly"},
        new Object[]{Arrays.asList("sku_premium", "sku_extended2professional_monthly", "sku_premium2extended"), "sku_extended2professional_monthly"},
        new Object[]{Arrays.asList("sku_premium2extended", "sku_premium", "sku_extended2professional_monthly"), "sku_extended2professional_monthly"},
        new Object[]{Arrays.asList("sku_premium2extended", "sku_extended2professional_monthly", "sku_premium"), "sku_extended2professional_monthly"},
        new Object[]{Arrays.asList("sku_extended2professional_monthly", "sku_premium", "sku_premium2extended"), "sku_extended2professional_monthly"},
        new Object[]{Arrays.asList("sku_extended2professional_monthly", "sku_premium2extended", "sku_premium"), "sku_extended2professional_monthly"},

        new Object[]{Arrays.asList("sku_extended", "sku_extended2professional_yearly"), "sku_extended2professional_yearly"},
        new Object[]{Arrays.asList("sku_extended2professional_yearly", "sku_extended"), "sku_extended2professional_yearly"},
    };
  }


  private LicenceStatus parse(String licenceStatus) {
    try {
      return LicenceStatus.valueOf(licenceStatus);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}