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

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.provider.filter.WhereFilter.Operation;

import java.text.DateFormat;
import java.util.Date;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;

public class DateCriteria extends Criteria {
  static final String COLUMN = KEY_DATE;


  /**
   * filters transactions up to or from the provided value, depending on operation
   * @param operation either {@link Operation#LTE} or {@link Operation#GTE}
   * @param value1
   */
  public DateCriteria(Operation operation, long value1) {
    super(
        operation,
        String.valueOf(value1));
  }

  /**
   * filters transaction between the provided values
   * @param value1
   * @param value2
   */
  public DateCriteria(long value1,long value2) {
    super(
        Operation.BTW,
        String.valueOf(value1),
        String.valueOf(value2));
  }

  @Override
  public int getID() {
    return R.id.FILTER_DATE_COMMAND;
  }

  @Override
  String getColumn() {
    return COLUMN;
  }

  public DateCriteria(Parcel in) {
    super(in);
  }
  public static final Creator<DateCriteria> CREATOR = new Creator<DateCriteria>() {
    public DateCriteria createFromParcel(Parcel in) {
        return new DateCriteria(in);
    }

    public DateCriteria[] newArray(int size) {
        return new DateCriteria[size];
    }
  };


  public static DateCriteria fromStringExtra(String extra) {
    String[] values = extra.split(EXTRA_SEPARATOR);
    Operation op = Operation.valueOf(values[0]);
    switch (op) {
      case BTW:
        return new DateCriteria(
            Long.parseLong(values[1]),
            Long.parseLong(values[2]));
      default:
        return new DateCriteria(
            op,
            Long.parseLong(values[1]));
    }
  }

  @Override
  public String toStringExtra() {
    String result = operation.name()+EXTRA_SEPARATOR+values[0];
    if (operation == Operation.BTW) {
      result += EXTRA_SEPARATOR+values[1];
    }
    return result;
  }
  @Override
  public String prettyPrint(Context context) {
    String result = "";
    DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
    String date1 = df.format(new Date(Long.valueOf(values[0])*1000L));
    switch (operation) {
      case GTE:
        result = MyApplication.getInstance().getString(R.string.after,date1);
        break;
      case LTE:
        result = MyApplication.getInstance().getString(R.string.before,date1);
        break;
      case BTW:
        String date2 = df.format(new Date(Long.valueOf(values[1])*1000L));
        result += MyApplication.getInstance().getString(R.string.between_and,date1,date2);
    }
    return result;
  }
  @Override
  protected boolean shouldApplyToParts() {
    return false;
  }
}
