package org.totschnig.myexpenses.model;

import android.content.Context;
import android.database.Cursor;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.util.TextUtils;
import org.totschnig.myexpenses.util.Utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_DAY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_WEEK;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_YEAR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_YEAR_OF_WEEK_START;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_WEEK_END;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_WEEK_START;

/**
 * grouping of transactions
 */
public enum Grouping {
  NONE, DAY, WEEK, MONTH, YEAR;

  /**
   * @param groupYear   the year of the group to display
   * @param groupSecond the number of the group in the second dimension (day, week or month)
   * @param c           a cursor where we can find information about the current date
   * @return a human readable String representing the group as header or activity title
   */
  public String getDisplayTitle(Context ctx, int groupYear, int groupSecond, Cursor c) {
    Calendar cal;
    switch (this) {
      case NONE:
        return ctx.getString(R.string.menu_aggregates);
      case DAY: {
        int this_day = c.getInt(c.getColumnIndex(KEY_THIS_DAY));
        int this_year = c.getInt(c.getColumnIndex(KEY_THIS_YEAR));
        cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, groupYear);
        cal.set(Calendar.DAY_OF_YEAR, groupSecond);
        String title = DateFormat.getDateInstance(DateFormat.FULL, MyApplication.getUserPreferedLocale()).format(cal.getTime());
        if (groupYear == this_year) {
          if (groupSecond == this_day)
            return ctx.getString(R.string.grouping_today) + " (" + title + ")";
          else if (groupSecond == this_day - 1)
            return ctx.getString(R.string.grouping_yesterday) + " (" + title + ")";
        }
        return title;
      }
      case WEEK: {
        int this_week = c.getInt(c.getColumnIndex(KEY_THIS_WEEK));
        int this_year_of_week_start = c.getInt(c.getColumnIndex(KEY_THIS_YEAR_OF_WEEK_START));
        DateFormat dateformat = Utils.localizedYearlessDateFormat();
        String weekRange = " (" + Utils.convDateTime(c.getLong(c.getColumnIndex(KEY_WEEK_START)), dateformat)
            + " - " + Utils.convDateTime(c.getLong(c.getColumnIndex(KEY_WEEK_END)), dateformat) + " )";
        String yearPrefix;
        if (groupYear == this_year_of_week_start) {
          if (groupSecond == this_week)
            return ctx.getString(R.string.grouping_this_week) + weekRange;
          else if (groupSecond == this_week - 1)
            return ctx.getString(R.string.grouping_last_week) + weekRange;
          yearPrefix = "";
        } else
          yearPrefix = groupYear + ", ";
        return yearPrefix + ctx.getString(R.string.grouping_week) + " " + groupSecond + weekRange;
      }
      case MONTH: {
        return getDisplayTitleForMonth(groupYear, groupSecond, DateFormat.LONG);
      }
      case YEAR:
        return String.valueOf(groupYear);
      default:
        return null;
    }
  }

  public static String getDisplayTitleForMonth(int groupYear, int groupSecond, int style) {
    final Locale userPreferedLocale = MyApplication.getUserPreferedLocale();
    int monthStarts = Integer.parseInt(PrefKey.GROUP_MONTH_STARTS.getString("1"));
    Calendar cal = Calendar.getInstance();
    if (monthStarts == 1) {
      cal.set(groupYear, groupSecond, 1);
      //noinspection SimpleDateFormat
      return new SimpleDateFormat("MMMM y", userPreferedLocale).format(cal.getTime());
    } else {
      DateFormat dateformat = DateFormat.getDateInstance(style, userPreferedLocale);
      cal = Calendar.getInstance();
      cal.set(groupYear, groupSecond, 1);
      if (cal.getActualMaximum(Calendar.DAY_OF_MONTH) < monthStarts) {
        cal.set(groupYear, groupSecond + 1, 1);
      } else {
        cal.set(Calendar.DATE, monthStarts);
      }
      String startDate = dateformat.format(cal.getTime());
      int endYear = groupYear, endMonth = groupSecond + 1;
      if (endMonth > Calendar.DECEMBER) {
        endMonth = Calendar.JANUARY;
        endYear++;
      }
      cal = Calendar.getInstance();
      cal.set(endYear, endMonth, 1);
      if (cal.getActualMaximum(Calendar.DAY_OF_MONTH) < monthStarts - 1) {
        cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
      } else {
        cal.set(Calendar.DATE, monthStarts - 1);
      }
      String endDate = dateformat.format(cal.getTime());
      String monthRange = " (" + startDate + " - " + endDate + " )";
      return monthRange;
    }
  }

  public static final String JOIN;

  static {
    JOIN = TextUtils.joinEnum(Grouping.class);
  }
}
