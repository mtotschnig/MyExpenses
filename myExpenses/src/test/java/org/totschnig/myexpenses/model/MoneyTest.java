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

import junit.framework.Assert;
import junit.framework.TestCase;

import java.math.BigDecimal;
import java.util.Currency;

public class MoneyTest extends TestCase {
  Currency c;
  Money m;

  /**
   * test a Currency with 2 FractionDigits
   */
  public void testEUR() {
    c = Currency.getInstance("EUR");
    m = new Money(c, (long) 2345);
    Assert.assertEquals(m.getAmountMinor().longValue(), (long) 2345);
    Assert.assertEquals(0, m.getAmountMajor().compareTo(new BigDecimal("23.45")));
    m.setAmountMajor(new BigDecimal("34.56"));
    Assert.assertEquals(m.getAmountMinor().longValue(), (long) 3456);
  }

  /**
   * test a Currency with 3 FractionDigits
   */
  public void testBHD() {
    c = Currency.getInstance("BHD");
    m = new Money(c, (long) 2345);
    Assert.assertEquals(m.getAmountMinor().longValue(), (long) 2345);
    Assert.assertEquals(0, m.getAmountMajor().compareTo(new BigDecimal("2.345")));
    m.setAmountMajor(new BigDecimal("3.456"));
    Assert.assertEquals(m.getAmountMinor().longValue(), (long) 3456);
  }

  /**
   * test a Currency with 0 FractionDigits
   */
  public void testJPY() {
    c = Currency.getInstance("JPY");
    m = new Money(c, (long) 2345);
    Assert.assertEquals(m.getAmountMinor().longValue(), (long) 2345);
    Assert.assertEquals(0, m.getAmountMajor().compareTo(new BigDecimal("2345")));
    m.setAmountMajor(new BigDecimal("3456"));
    Assert.assertEquals(m.getAmountMinor().longValue(), (long) 3456);
  }

  /**
   * test no Currency
   */
  public void testXXX() {
    c = Currency.getInstance("XXX");
    long minor = (long) (2345 * Math.pow(10, Money.DEFAULTFRACTIONDIGITS));
    m = new Money(c, minor);
    Assert.assertEquals(m.getAmountMinor().longValue(), minor);
    Assert.assertEquals(0, m.getAmountMajor().compareTo(new BigDecimal("2345")));
    m.setAmountMajor(new BigDecimal("3456.789"));
    Assert.assertEquals(m.getAmountMinor().longValue(), (long) (3456.789 * Math.pow(10, Money.DEFAULTFRACTIONDIGITS)));
  }

  public void testBuildWithMicrosEUR() {
    c = Currency.getInstance("EUR");
    assertEquals(2, Money.getFractionDigits(c));
    m = Money.buildWithMicros(c, 23450000);
    Assert.assertEquals(2345L, m.getAmountMinor().longValue());
  }

  public void testBuildWithMicrosBHD() {
    c = Currency.getInstance("BHD");
    assertEquals(3, Money.getFractionDigits(c));
    m = Money.buildWithMicros(c, 23450000);
    Assert.assertEquals(23450L, m.getAmountMinor().longValue());
  }

  public void testBuildWithMicrosJPY() {
    c = Currency.getInstance("JPY");
    assertEquals(0, Money.getFractionDigits(c));
    m = Money.buildWithMicros(c, 23450000);
    Assert.assertEquals(23L, m.getAmountMinor().longValue());
  }

  public void testBuildWithMicrosXXX() {
    c = Currency.getInstance("XXX");
    assertEquals(8, Money.getFractionDigits(c));
    m = Money.buildWithMicros(c, 23450000);
    Assert.assertEquals(2345000000L, m.getAmountMinor().longValue());
  }
}
