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

import org.totschnig.myexpenses.util.Utils;

import java.io.Serializable;
import java.math.BigDecimal;

import androidx.annotation.NonNull;

public class Money implements Serializable {
  private CurrencyUnit currencyUnit;
  private Long amountMinor;

  public Money(CurrencyUnit currencyUnit, @NonNull Long amountMinor) {
    this.currencyUnit = currencyUnit;
    this.amountMinor = amountMinor;
  }

  public Money(CurrencyUnit currencyUnit, @NonNull BigDecimal amountMajor) {
    this.currencyUnit = currencyUnit;
    setAmountMajor(amountMajor);
  }

  public CurrencyUnit getCurrencyUnit() {
    return currencyUnit;
  }

  public Long getAmountMinor() {
    return amountMinor;
  }

  private void setAmountMajor(BigDecimal amountMajor) {
    this.amountMinor = amountMajor.movePointRight(currencyUnit.fractionDigits()).longValue();
  }

  public BigDecimal getAmountMajor() {
    return new BigDecimal(amountMinor).movePointLeft(currencyUnit.fractionDigits());
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
    if (currencyUnit == null) {
      if (other.currencyUnit != null)
        return false;
    } else if (!currencyUnit.equals(other.currencyUnit))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result = this.currencyUnit != null ? this.currencyUnit.hashCode() : 0;
    result = 31 * result + (this.amountMinor != null ? this.amountMinor.hashCode() : 0);
    return result;
  }

  /**
   * Builds a Money instance where amount is provided in micro units (=1/1000000 of the main unit)
   *
   * @return a new Money object
   */
  public static Money buildWithMicros(CurrencyUnit currency, long amountMicros) {
    long amountMinor;
    int fractionDigits = currency.fractionDigits();
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
