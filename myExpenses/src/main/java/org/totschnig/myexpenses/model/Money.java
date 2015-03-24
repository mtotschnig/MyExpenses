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

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.util.Utils;

import android.util.Log;

public class Money implements Serializable {
  public static final String KEY_CUSTOM_FRACTION_DIGITS = "CustomFractionDigits";
  private Currency currency;
  private Long amountMinor;
  private int fractionDigits;
  /**
   * used with currencies where Currency.getDefaultFractionDigits returns -1
   */
  public static int DEFAULTFRACTIONDIGITS = 8;
  
  public Money(Currency currency, Long amountMinor) {
    this.currency = currency;
    this.amountMinor = amountMinor;
    this.fractionDigits = fractionDigits(currency);
  }
  public Money(Currency currency, BigDecimal amountMajor) {
    setCurrency(currency);
    setAmountMajor(amountMajor);
  }
  public Currency getCurrency() {
    return currency;
  }
  public void setCurrency(Currency currency) {
    this.currency = currency;
    this.fractionDigits = fractionDigits(currency);
  }
  public Long getAmountMinor() {
    return amountMinor;
  }
  public void setAmountMinor(Long amountMinor) {
    this.amountMinor = amountMinor;
  }
  public void setAmountMajor(BigDecimal amountMajor) {
    this.amountMinor = amountMajor.multiply(new BigDecimal(Math.pow(10,fractionDigits))).longValue();
  }
  public BigDecimal getAmountMajor() {
    BigDecimal bd = new BigDecimal(amountMinor);
    try {
      return bd.divide(new BigDecimal(Math.pow(10,fractionDigits)));
    } catch (ArithmeticException e) {
      Utils.reportToAcra(new RuntimeException(
          "Error calculating amount major for : "+amountMinor+" with "+fractionDigits + "fraction digits.",e));
      return bd.divide(new BigDecimal(Math.pow(10,fractionDigits)),fractionDigits,RoundingMode.DOWN);
    }
  }
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Money other = (Money) obj;
    if (amountMinor == null) {
      if (other.amountMinor != null)
        return false;
    } else if (!amountMinor.equals(other.amountMinor))
      return false;
    if (currency == null) {
      if (other.currency != null)
        return false;
    } else if (!currency.equals(other.currency))
      return false;
    return true;
  }
  /**
   * @param c
   * @return getDefaultFractionDigits for a currency, unless it is -1,
   * then we return {@link Money#DEFAULTFRACTIONDIGITS} in order to allow fractions with currencies like XXX
   */
  public static int fractionDigits(Currency c) {
    MyApplication context = MyApplication.getInstance();
    //in testing environment context might be null
    if (context!=null) {
      int customFractionDigits = context.getSettings()
          .getInt(c.getCurrencyCode()+KEY_CUSTOM_FRACTION_DIGITS, -1);
      if (customFractionDigits != -1) {
        return customFractionDigits;
      }
    }
    int digits = c.getDefaultFractionDigits();
    if (digits != -1) {
      return digits;
    }
    return DEFAULTFRACTIONDIGITS;
  }
}
