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

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.provider.filter.WhereFilter.Operation;
import org.totschnig.myexpenses.util.CurrencyFormatter;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;

public class AmountCriteria extends Criteria {
  static final String COLUMN = KEY_AMOUNT;
  private boolean type;
  private String currency;
  private Operation origOperation;
  private Long origValue1, origValue2;

  public AmountCriteria(Operation operation, String currency, boolean type, Long... values) {
    super(transformCriteria(operation, type, values));
    this.type = type;
    this.currency = currency;
    this.origOperation = operation;
    this.origValue1 = values[0];
    this.origValue2 = values[1];
  }

  @Override
  public int getID() {
    return R.id.FILTER_AMOUNT_COMMAND;
  }

  @Override
  String getColumn() {
    return COLUMN;
  }

  private AmountCriteria(Parcel in) {
    super(in);
    type = in.readByte() != 0;
    currency = in.readString();
    origOperation = Operation.valueOf(in.readString());
    origValue1 = in.readLong();
    if (origOperation == Operation.BTW) {
      origValue2 = in.readLong();
    }
  }

  @Override
  public String prettyPrint(Context context) {
    CurrencyFormatter currencyFormatter = ((MyApplication) context.getApplicationContext()).getAppComponent().currencyFormatter();
    String result = context.getString(type ? R.string.income : R.string.expense) + " ";
    CurrencyContext currencyContext = ((MyApplication) context.getApplicationContext()).getAppComponent().currencyContext();
    CurrencyUnit currencyUnit = currencyContext.get(currency);
    String amount1 = currencyFormatter.formatCurrency(new Money(currencyUnit, Math.abs(origValue1)));
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
        String amount2 = currencyFormatter.formatCurrency(new Money(currencyUnit, Math.abs(origValue2)));
        result += context.getString(R.string.between_and, amount1, amount2);
    }
    return result;
  }

  private static CriteriaInfo transformCriteria(Operation operation, boolean type, Long... values) {
    switch (operation) {
      case BTW:
      case EQ:
      case GTE:
      case LTE:
        break;
      default:
        throw new UnsupportedOperationException("Operator not supported: " + operation.name());
    }
    long longAmount1, longAmount2;
    longAmount1 =  type ? values[0] : -values[0];
    if (operation == Operation.BTW) {
      if (values[1] == null) {
        throw new UnsupportedOperationException("Operator BTW needs two values");
      }
      longAmount2 = type ? values[1] : -values[1];
      boolean needSwap = longAmount2 < longAmount1;
      return new CriteriaInfo(
          WhereFilter.Operation.BTW,
          String.valueOf(needSwap ? longAmount2 : longAmount1),
          String.valueOf(needSwap ? longAmount1 : longAmount2));
    }
    if (type) {
      if (operation == Operation.LTE) {
        return new CriteriaInfo(
            Operation.BTW,
            "0",
            String.valueOf(longAmount1));
      }
    } else {
      if (operation == Operation.GTE) {
        operation = Operation.LTE;
      } else if (operation == Operation.LTE) {
        return new CriteriaInfo(
            Operation.BTW,
            String.valueOf(longAmount1),
            "0");
      }
    }
    return new CriteriaInfo(
        operation,
        String.valueOf(longAmount1));
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeByte((byte) (type ? 1 : 0));
    dest.writeString(currency);
    dest.writeString(origOperation.name());
    dest.writeLong(origValue1);
    if (origOperation == Operation.BTW) {
      dest.writeLong(origValue2);
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
    String result = origOperation.name() + EXTRA_SEPARATOR + currency + EXTRA_SEPARATOR + (type ? "1" : "0") + EXTRA_SEPARATOR + origValue1;
    if (origOperation == Operation.BTW) {
      result += EXTRA_SEPARATOR + origValue2;
    }
    return result;
  }

  public static AmountCriteria fromStringExtra(String extra) {
    String[] values = extra.split(EXTRA_SEPARATOR);
    Operation op = Operation.valueOf(values[0]);
    try {
      return new AmountCriteria(
          op,
          values[1],
          values[2].equals("1"),
          Long.valueOf(values[3]),
          op == Operation.BTW ? Long.valueOf(values[4]) : null);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
