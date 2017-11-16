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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.util.Utils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public class Money implements Serializable {
  private static final String KEY_CUSTOM_FRACTION_DIGITS = "CustomFractionDigits";
  private static final String KEY_CUSTOM_CURRENCY_SYMBOL = "CustomCurrencySymbol";
  private Currency currency;
  private Long amountMinor;
  private int fractionDigits;
  /**
   * used with currencies where Currency.getDefaultFractionDigits returns -1
   */
  public static final int DEFAULTFRACTIONDIGITS = 8;

  public Money(Currency currency, Long amountMinor) {
    this.currency = currency;
    this.amountMinor = amountMinor;
    this.fractionDigits = getFractionDigits(currency);
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
    this.fractionDigits = getFractionDigits(currency);
  }

  public Long getAmountMinor() {
    return amountMinor;
  }

  public void setAmountMinor(Long amountMinor) {
    this.amountMinor = amountMinor;
  }

  public void setAmountMajor(BigDecimal amountMajor) {
    this.amountMinor = amountMajor.multiply(new BigDecimal(Math.pow(10, fractionDigits))).longValue();
  }

  public BigDecimal getAmountMajor() {
    return new BigDecimal(amountMinor).divide(
        new BigDecimal(Math.pow(10, fractionDigits)),
        fractionDigits,
        RoundingMode.DOWN);
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

  @Override
  public int hashCode() {
    int result = this.currency != null ? this.currency.hashCode() : 0;
    result = 31 * result + (this.amountMinor != null ? this.amountMinor.hashCode() : 0);
    result = 31 * result + this.fractionDigits;
    return result;
  }

  /**
   * @param c
   * @return getDefaultFractionDigits for a currency, unless it is -1,
   * then we return {@link Money#DEFAULTFRACTIONDIGITS} in order to allow fractions with currencies like XXX
   */
  public static int getFractionDigits(Currency c) {
    MyApplication context = MyApplication.getInstance();
    //in testing environment context might be null
    if (context != null) {
      int customFractionDigits = context.getSettings()
          .getInt(c.getCurrencyCode() + KEY_CUSTOM_FRACTION_DIGITS, -1);
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

  public static void storeCustomFractionDigits(String currencyCode, int newValue) {
    MyApplication.getInstance().getSettings().edit()
        .putInt(currencyCode + KEY_CUSTOM_FRACTION_DIGITS, newValue).apply();
  }

  public static void ensureFractionDigitsAreCached(Currency currency) {
    storeCustomFractionDigits(currency.getCurrencyCode(), getFractionDigits(currency));
  }

  @Nullable
  public static String getCustomSymbol(@NonNull Currency currencyCode) {
    return MyApplication.getInstance().getSettings()
        .getString(currencyCode + KEY_CUSTOM_CURRENCY_SYMBOL, null);
  }

  @NonNull
  public static String getSymbol(@NonNull Currency currency) {
    String custom = getCustomSymbol(currency);
    return custom != null ? custom : currency.getSymbol();
  }

  public static boolean storeCustomSymbol(String currencyCode, String symbol) {
    if (!Currency.getInstance(currencyCode).getSymbol().equals(symbol)) {
      MyApplication.getInstance().getSettings().edit()
          .putString(currencyCode + KEY_CUSTOM_CURRENCY_SYMBOL, symbol).apply();
      return true;
    }
    return false;
  }

  /**
   * Builds a Money instance where amount is provided in micro units (=1/1000000 of the main unit)
   *
   * @return a new Money object
   */
  public static Money buildWithMicros(Currency currency, long amountMicros) {
    long amountMinor;
    int fractionDigits = getFractionDigits(currency);
    switch (Utils.compare(6, fractionDigits)) {
      case -1:
        amountMinor = amountMicros * (long) Math.pow(10, fractionDigits - 6);
        break;
      case 1:
        amountMinor = amountMicros / (long) Math.pow(10, 6 - fractionDigits);
        break;
      default:
        amountMinor = amountMicros;
    }
    return new Money(currency, amountMinor);
  }
}
