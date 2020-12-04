package org.totschnig.myexpenses.export.qif;

import junit.framework.TestCase;

import org.totschnig.myexpenses.model.CurrencyUnit;

import java.math.BigDecimal;
import java.util.Currency;

public class QifUtilParseMoneyTest extends TestCase {

  private CurrencyUnit eur;

  @Override
  protected void setUp() {
    eur = new CurrencyUnit(Currency.getInstance("EUR"));
  }

  public void testShouldParseCommaAsDecimalSeparator() {
    assertEquals(new BigDecimal("4.13"), QifUtils.parseMoney("4,13", eur));
  }

  public void testShouldParseDotAsDecimalSeparator() {
    assertEquals(new BigDecimal("4.13"), QifUtils.parseMoney("4.13", eur));
  }

  public void testShouldTakeIntoAccountMultipleGroups() {
    assertEquals(new BigDecimal("230820162308.2016"), QifUtils.parseMoney("23.08.2016;23.08.2016;blabla", eur));
  }

  public void testShouldNotExceedLimitWithMultipleGroups() {
    try {
      QifUtils.parseMoney("23.08.2016;23.08.2016;blabla",11);
      fail("Should throw IllegalArgumentException");
    } catch(IllegalArgumentException e) {}
  }

  public void testShouldIgnoreBlanks() {
    assertEquals(new BigDecimal("230820.16"), QifUtils.parseMoney("23 08 20,16", eur));
  }

  public void testShouldStartFromFirstRelevantGroup() {
    assertEquals(new BigDecimal("230820.16"), QifUtils.parseMoney("blabla230820,16", eur));
  }

  public void testShouldNotExceedLimit() {
    assertEquals(new BigDecimal(1234567890), QifUtils.parseMoney("1234567890", 10));
    try {
      QifUtils.parseMoney("1234567890",9);
      fail("Should throw IllegalArgumentException");
    } catch(IllegalArgumentException e) {}
  }

  public void testShouldParseMoneyWithGroupSeparator() {
    assertEquals(new BigDecimal("-2600.66"), QifUtils.parseMoney("-2,600.66", eur));
  }
}
