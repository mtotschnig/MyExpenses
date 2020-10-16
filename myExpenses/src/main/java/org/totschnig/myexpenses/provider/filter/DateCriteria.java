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

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.FormatStyle;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.provider.filter.WhereFilter.Operation;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;
import static org.totschnig.myexpenses.util.DateUtilsKt.localDateTime2Epoch;

public class DateCriteria extends Criteria {
  static final String COLUMN = KEY_DATE;


  /**
   * filters transactions up to or from the provided value, depending on operation
   *
   * @param operation either {@link Operation#LTE} or {@link Operation#GTE}
   * @param value1
   */
  public DateCriteria(Operation operation, LocalDate value1) {
    super(operation, value1.toString());
  }

  /**
   * filters transaction between the provided values
   *
   * @param value1
   * @param value2
   */
  public DateCriteria(LocalDate value1, LocalDate value2) {
    super(Operation.BTW, value1.toString(), value2.toString());
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
    if (op == Operation.BTW) {
      return new DateCriteria(LocalDate.parse(values[1]), LocalDate.parse(values[2]));
    }
    return new DateCriteria(op, LocalDate.parse(values[1]));
  }

  public static DateCriteria fromLegacy(String extra) {
    String[] values = extra.split(EXTRA_SEPARATOR);
    Operation op = Operation.valueOf(values[0]);
    if (op == Operation.BTW) {
      return new DateCriteria(fromEpoch(values[1]), fromEpoch(values[2]));
    }
    return new DateCriteria(op, fromEpoch(values[1]));
  }

  private static LocalDate fromEpoch(String epoch) {
    return Instant.ofEpochSecond(Long.parseLong(epoch)).atZone(ZoneId.systemDefault()).toLocalDate();
  }

  @Override
  public String toStringExtra() {
    String result = operation.name() + EXTRA_SEPARATOR + values[0];
    if (operation == Operation.BTW) {
      result += EXTRA_SEPARATOR + values[1];
    }
    return result;
  }

  @Override
  public String[] getSelectionArgs() {
    switch (operation) {
      case GTE:
        return new String[]{toStartOfDay(values[0])};
      case LTE:
        return new String[]{toEndOfDay(values[0])};
      case BTW:
        return new String[]{toStartOfDay(values[0]), toEndOfDay(values[1])};
      default:
        throw new IllegalStateException("Unexpected value: " + operation);
    }
  }

  private String toStartOfDay(String localDate) {
    return local2ZonedAtTime(localDate, LocalTime.MIN);
  }

  private String toEndOfDay(String localDate) {
    return local2ZonedAtTime(localDate, LocalTime.MAX);
  }

  private String local2ZonedAtTime(String localDate, LocalTime localTime) {
    return String.valueOf(localDateTime2Epoch(LocalDate.parse(localDate).atTime(localTime)));
  }

  @Override
  public String prettyPrint(Context context) {
    String result = "";
    DateTimeFormatter df = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
    String date1 = df.format(LocalDate.parse(values[0]));
    switch (operation) {
      case GTE:
        result = context.getString(R.string.after, date1);
        break;
      case LTE:
        result = context.getString(R.string.before, date1);
        break;
      case BTW:
        String date2 = df.format(LocalDate.parse(values[1]));
        result += context.getString(R.string.between_and, date1, date2);
    }
    return result;
  }

  @Override
  protected boolean shouldApplyToParts() {
    return false;
  }
}
