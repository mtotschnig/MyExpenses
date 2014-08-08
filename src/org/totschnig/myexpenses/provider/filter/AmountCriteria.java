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
 *   
 *   Based on Financisto (c) 2010 Denis Solonenko, made available
 *   under the terms of the GNU Public License v2.0
*/

package org.totschnig.myexpenses.provider.filter;

import java.math.BigDecimal;
import java.util.Currency;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.AmountActivity;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.filter.WhereFilter.Operation;
import org.totschnig.myexpenses.util.Utils;

import android.os.Parcel;
import android.os.Parcelable;

public class AmountCriteria extends Criteria {
  private boolean type;
  private Currency currency;
  private Operation origOperation;
  private BigDecimal origValue1,origValue2;

  public AmountCriteria(Operation operation, Currency currency, boolean type, BigDecimal... values) {
    super(transformCriteria(operation,currency,type,values));
    this.type = type;
    this.currency = currency;
    this.origOperation=operation;
    this.origValue1=values[0];
    this.origValue2=values[1];
    this.title = MyApplication.getInstance().getString(R.string.amount);
  }
  public AmountCriteria(Parcel in) {
    super(in);
    type = in.readByte() != 0;
    currency = Currency.getInstance(in.readString());
    origOperation = Operation.valueOf(in.readString());
    origValue1 = new BigDecimal(in.readString());
    if (origOperation==Operation.BTW) {
      origValue2 = new BigDecimal(in.readString());
    }
  }
  @Override
  public String prettyPrint() {
    String result = MyApplication.getInstance().getString(
        type == AmountActivity.EXPENSE ? R.string.expense : R.string.income) + " ";
    String amount1 = Utils.formatCurrency(new Money(currency,origValue1.abs()));
    switch (origOperation) {
    case EQ:
      result += "= " + amount1;
      break;
    case GTE:
      result += "≥ " + amount1;
      break;
    case LTE:
      result += "≤ " + amount1;
      break;
    case BTW:
      String amount2 = Utils.formatCurrency(new Money(currency,origValue2.abs()));
      result += MyApplication.getInstance().getString(R.string.between_and,amount1,amount2);
    }
    return prettyPrintInternal(result);
  }
  private static Criteria transformCriteria(Operation operation, Currency currency, boolean type,BigDecimal... values) {
    switch(operation) {
    case BTW:
    case EQ:
    case GTE:
    case LTE:
      break;
    default:
      throw new UnsupportedOperationException("Operator not supported: "+operation.name());
    }
    Long longAmount1,longAmount2;
    longAmount1 = new Money(
        currency,
        type == AmountActivity.EXPENSE ? values[0].negate() : values[0])
      .getAmountMinor();
    if (operation==Operation.BTW) {
      if (values[1]==null) {
        throw new UnsupportedOperationException("Operator BTW needs two values");
      }
      longAmount2 = new Money(
          currency,
          type == AmountActivity.EXPENSE ? values[1].negate() : values[1])
        .getAmountMinor();
      boolean needSwap = longAmount2<longAmount1;
      return new Criteria(
          DatabaseConstants.KEY_AMOUNT,
          WhereFilter.Operation.BTW,
          String.valueOf(needSwap?longAmount2:longAmount1),
          String.valueOf(needSwap?longAmount1:longAmount2));
    }
    if (type == AmountActivity.EXPENSE) {
      if (operation==Operation.GTE) {
        operation=Operation.LTE;
      } else if (operation==Operation.LTE) {
        return new Criteria(
            DatabaseConstants.KEY_AMOUNT,
            WhereFilter.Operation.BTW,
            String.valueOf(longAmount1),
            "0");
      }
    } else {
      if (operation==Operation.LTE) {
        return new Criteria(
            DatabaseConstants.KEY_AMOUNT,
            WhereFilter.Operation.BTW,
            "0",
            String.valueOf(longAmount1));
      }
    }
    return new Criteria(
        DatabaseConstants.KEY_AMOUNT,
        operation,
        String.valueOf(longAmount1));
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    // TODO Auto-generated method stub
    super.writeToParcel(dest, flags);
    dest.writeByte((byte) (type ? 1 : 0));
    dest.writeString(currency.getCurrencyCode());
    dest.writeString(origOperation.name());
    dest.writeString(origValue1.toPlainString());
    if (origOperation == Operation.BTW) {
      dest.writeString(origValue2.toPlainString());
    }
  }

  public static final Parcelable.Creator<AmountCriteria> CREATOR = new Parcelable.Creator<AmountCriteria>() {
    public AmountCriteria createFromParcel(Parcel in) {
        return new AmountCriteria(in);
    }

    public AmountCriteria[] newArray(int size) {
        return new AmountCriteria[size];
    }
  };

  @Override
  public String toStringExtra() {
    String result = origOperation.name()+EXTRA_SEPARATOR+currency.getCurrencyCode()+EXTRA_SEPARATOR+(type?"1":"0")+EXTRA_SEPARATOR+origValue1.toPlainString();
    if (origOperation == Operation.BTW) {
      result += EXTRA_SEPARATOR+origValue2.toPlainString();
    }
    return result;
  }

  public static AmountCriteria fromStringExtra(String extra) {
    String[] values = extra.split(EXTRA_SEPARATOR);
    Operation op = Operation.valueOf(values[0]);
    return new AmountCriteria(
        op,
        Currency.getInstance(values[1]),
        values[2].equals("1"),
        new BigDecimal(values[3]),
        op==Operation.BTW?new BigDecimal(values[4]):null);
  }
}
