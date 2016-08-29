package org.totschnig.myexpenses.export.qif;

import junit.framework.TestCase;

import java.math.BigDecimal;

public class QifUtilParseMoneyTest extends TestCase {

  public void testShouldParseCommaAsDecimalSeparator() {
    assertEquals(new BigDecimal("4.13"), QifUtils.parseMoney("4,13"));
  }

  public void testShouldParseDotAsDecimalSeparator() {
    assertEquals(new BigDecimal("4.13"), QifUtils.parseMoney("4.13"));
  }

  public void testShouldTakeIntoAccountTwoGroups() {
    assertEquals(new BigDecimal("23.08"), QifUtils.parseMoney("23.08.2016;23.08.2016;blabla"));
  }

  public void testShouldIgnoreBlanks() {
    assertEquals(new BigDecimal("230820.16"), QifUtils.parseMoney("23 08 20,16"));
  }
  
  public void testShouldStartFromFirstRelevantGroup() {
    assertEquals(new BigDecimal("230820.16"), QifUtils.parseMoney("blabla230820,16"));
  }
}
