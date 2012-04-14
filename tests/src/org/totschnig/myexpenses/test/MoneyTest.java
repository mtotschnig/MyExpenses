package org.totschnig.myexpenses.test;

import java.math.BigDecimal;
import java.util.Currency;

import org.totschnig.myexpenses.Money;

import junit.framework.Assert;
import junit.framework.TestCase;

public class MoneyTest extends TestCase {
  Currency c;
  Money m;
  /**
   * test a Currency with 2 FractionDigits
   */
  public void testEUR() {
    c = Currency.getInstance("EUR");
    m = new Money(c,(long) 2345);
    Assert.assertEquals(m.getAmountMinor().longValue(),(long)2345);
    Assert.assertEquals(0,m.getAmountMajor().compareTo(new BigDecimal("23.45")));
    m.setAmountMajor(new BigDecimal("34.56"));
    Assert.assertEquals(m.getAmountMinor().longValue(),(long)3456);
  }
  /**
   * test a Currency with 3 FractionDigits
   */
  public void testBHD() {
    c = Currency.getInstance("BHD");
    m = new Money(c,(long) 2345);
    Assert.assertEquals(m.getAmountMinor().longValue(),(long)2345);
    Assert.assertEquals(0,m.getAmountMajor().compareTo(new BigDecimal("2.345")));
    m.setAmountMajor(new BigDecimal("3.456"));
    Assert.assertEquals(m.getAmountMinor().longValue(),(long)3456);
  }
  /**
   * test a Currency with 0 FractionDigits
   */
  public void testJPY() {
    c = Currency.getInstance("JPY");
    m = new Money(c,(long) 2345);
    Assert.assertEquals(m.getAmountMinor().longValue(),(long)2345);
    Assert.assertEquals(0,m.getAmountMajor().compareTo(new BigDecimal("2345")));
    m.setAmountMajor(new BigDecimal("3456"));
    Assert.assertEquals(m.getAmountMinor().longValue(),(long)3456);
    }
}
