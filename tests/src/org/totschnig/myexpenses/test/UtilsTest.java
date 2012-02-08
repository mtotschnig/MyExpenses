package org.totschnig.myexpenses.test;

import java.util.Locale;

import org.totschnig.myexpenses.Utils;

import junit.framework.Assert;
import junit.framework.TestCase;

public class UtilsTest extends TestCase {
  public void testValidateNumber() {
    Locale.setDefault(Locale.ENGLISH);
    Assert.assertEquals(4.7f, Utils.validateNumber("4.7"));
    Assert.assertNull(Utils.validateNumber("4,7"));
    Locale.setDefault(Locale.FRENCH);
    Assert.assertEquals(4.7f, Utils.validateNumber("4,7"));
    Assert.assertNull(Utils.validateNumber("4.7"));
    Locale.setDefault(Locale.GERMAN);
    Assert.assertEquals(4.7f, Utils.validateNumber("4,7"));
    Assert.assertNull(Utils.validateNumber("4.7"));
  }
}
