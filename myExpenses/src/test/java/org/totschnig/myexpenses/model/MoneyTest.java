/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.totschnig.myexpenses.model;

import junit.framework.TestCase;

import java.math.BigDecimal;
import java.util.Currency;

public class MoneyTest extends TestCase {
  int DEFAULT_FRACTION_DIGITS = 8;
  CurrencyUnit c;
  Money m;

  /**
   * test a Currency with 2 FractionDigits
   */
  public void testEUR() {
    c = buildCurrencyUnit("EUR");
    m = new Money(c, (long) 2345);
    assertEquals(m.getAmountMinor(), (long) 2345);
    assertEquals(0, m.getAmountMajor().compareTo(new BigDecimal("23.45")));
    m = new Money(c, new BigDecimal("34.56"));
    assertEquals(m.getAmountMinor(), (long) 3456);
  }

  /**
   * test a Currency with 3 FractionDigits
   */
  public void testBHD() {
    c = buildCurrencyUnit("BHD");
    m = new Money(c, (long) 2345);
    assertEquals(m.getAmountMinor(), (long) 2345);
    assertEquals(0, m.getAmountMajor().compareTo(new BigDecimal("2.345")));
    m = new Money(c, new BigDecimal("3.456"));
    assertEquals(m.getAmountMinor(), (long) 3456);
  }

  /**
   * test a Currency with 0 FractionDigits
   */
  public void testJPY() {
    c = buildCurrencyUnit("JPY");
    m = new Money(c, (long) 2345);
    assertEquals(m.getAmountMinor(), (long) 2345);
    assertEquals(0, m.getAmountMajor().compareTo(new BigDecimal("2345")));
    m = new Money(c, new BigDecimal("3456"));
    assertEquals(m.getAmountMinor(), (long) 3456);
  }

  /**
   * test no Currency
   */
  public void testXXX() {
    c = buildXXX();
    long minor = (long) (2345 * Math.pow(10, DEFAULT_FRACTION_DIGITS));
    m = new Money(c, minor);
    assertEquals(m.getAmountMinor(), minor);
    assertEquals(0, m.getAmountMajor().compareTo(new BigDecimal("2345")));
    m = new Money(c, new BigDecimal("3456.789"));
    assertEquals(m.getAmountMinor(), (long) (3456.789 * Math.pow(10, DEFAULT_FRACTION_DIGITS)));
  }

  public void testBuildWithMicrosEUR() {
    c = buildCurrencyUnit("EUR");
    assertEquals(2, c.getFractionDigits());
    m = Money.buildWithMicros(c, 23450000);
    assertEquals(2345L, m.getAmountMinor());
  }

  public void testBuildWithMicrosBHD() {
    c = buildCurrencyUnit("BHD");
    assertEquals(3, c.getFractionDigits());
    m = Money.buildWithMicros(c, 23450000);
    assertEquals(23450L, m.getAmountMinor());
  }

  public void testBuildWithMicrosJPY() {
    c = buildCurrencyUnit("JPY");
    assertEquals(0, c.getFractionDigits());
    m = Money.buildWithMicros(c, 23450000);
    assertEquals(23L, m.getAmountMinor());
  }

  public void testBuildWithMicrosXXX() {
    c = buildXXX();
    assertEquals(8, c.getFractionDigits());
    m = Money.buildWithMicros(c, 23450000);
    assertEquals(2345000000L, m.getAmountMinor());
  }

  private CurrencyUnit buildCurrencyUnit(String code) {
    Currency currency = Currency.getInstance(code);
    return new CurrencyUnit(code, currency.getSymbol(), currency.getDefaultFractionDigits());
  }

  private CurrencyUnit buildXXX() {
    return new CurrencyUnit("XXX", "XXX", DEFAULT_FRACTION_DIGITS);
  }
}
