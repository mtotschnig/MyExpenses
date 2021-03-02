package org.totschnig.myexpenses.model;

import android.content.Context;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.util.TextUtils;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.viewmodel.data.DateInfo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * grouping of transactions
 */
public enum Grouping {
  NONE, DAY, WEEK, MONTH, YEAR;

  /**
   * @param groupYear   the year of the group to display
   * @param groupSecond the number of the group in the second dimension (day, week or month)
   * @param dateInfo           a cursor where we can find information about the current date
   * @param userPreferredLocale
   * @return a human readable String representing the group as header or activity title
   */
  @NonNull
  public String getDisplayTitle(@Nullable Context ctx, int groupYear, int groupSecond, DateInfo dateInfo, Locale userPreferredLocale) {
    if (ctx == null) {
      return "";
    }
    Calendar cal;
    switch (this) {
      case NONE:
        return ctx.getString(R.string.menu_aggregates);
      case DAY: {
        int this_day = dateInfo.getThisDay();
        int this_year = dateInfo.getThisYear();
        cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, groupYear);
        cal.set(Calendar.DAY_OF_YEAR, groupSecond);
        String title = DateFormat.getDateInstance(DateFormat.FULL, userPreferredLocale).format(cal.getTime());
        if (groupYear == this_year) {
          if (groupSecond == this_day)
            return ctx.getString(R.string.grouping_today) + " (" + title + ")";
          else if (groupSecond == this_day - 1)
            return ctx.getString(R.string.grouping_yesterday) + " (" + title + ")";
        }
        return title;
      }
      case WEEK: {
        int this_week = dateInfo.getThisWeek();
        int this_year_of_week_start = dateInfo.getThisYearOfWeekStart();
        DateFormat dateformat = Utils.localizedYearLessDateFormat(ctx);
        String weekRange = " (" + Utils.convDateTime(dateInfo.getWeekStart(), dateformat)
            + " - " + Utils.convDateTime(dateInfo.getWeekEnd(), dateformat) + " )";
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
        return getDisplayTitleForMonth(groupYear, groupSecond, DateFormat.LONG, userPreferredLocale);
      }
      case YEAR:
        return String.valueOf(groupYear);
      default:
        return null;
    }
  }

  public static String getDisplayTitleForMonth(int groupYear, int groupSecond, int style, Locale userPreferedLocale) {
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
