package org.totschnig.myexpenses.util;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.android.calendar.CalendarContractCompat;
import com.android.calendar.CalendarContractCompat.Events;

import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.provider.CalendarProviderProxy;
import org.totschnig.myexpenses.provider.DatabaseConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;

import static org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup.CALENDAR;

public class PlanInfoCursorWrapper extends CursorWrapperHelper {
  private Context context;
  private final HashMap<Long, String> planInfo = new HashMap<>();
  private final HashMap<Integer, Long> nextInstance = new HashMap<>();
  private ArrayList<Integer> sortedPositions = new ArrayList<>();
  private boolean isInitializingPlanInfo;
  private boolean shouldSortByNextInstance;

  public PlanInfoCursorWrapper(Context context, Cursor cursor, boolean shouldSortByNextInstance) {
    super(cursor);
    this.context = context;
    this.shouldSortByNextInstance = shouldSortByNextInstance;
    initializePlanInfo();
  }

  public void initializePlanInfo() {
    if (!CALENDAR.hasPermission(context)) {
      shouldSortByNextInstance = false;
      return;
    }
    isInitializingPlanInfo = true; // without having to support Gingerbread, we would not need to switch of sort,
                    // we would use getWrappedCursor method introduced later
    if (moveToFirst()) {
      ArrayList<Long> plans = new ArrayList<>();
      long planId;
      int columnIndexPlanId = getColumnIndex(DatabaseConstants.KEY_PLANID);
      while (!isAfterLast()) {
        if ((planId = getLong(columnIndexPlanId)) != 0L) {
          plans.add(planId);
          nextInstance.put(getPosition(), getNextInstance(planId));
        }
        sortedPositions.add(getPosition());
        moveToNext();
      }
      Collections.sort(sortedPositions, new Comparator<Integer>() {
        @Override
        public int compare(Integer lhs, Integer rhs) {
          Long lhNextInstance = nextInstance.get(lhs);
          Long rhNextInstance = nextInstance.get(rhs);
          if (lhNextInstance == null) {
            if (rhNextInstance == null) {
              return 0;
            } else {
              return 1;
            }
          }
          if (rhNextInstance == null) {
            return -1;
          }
          return lhNextInstance.compareTo(rhNextInstance);
        }
      });
      if (plans.size() > 0) {
        Cursor c = context.getContentResolver().query(Events.CONTENT_URI,
            new String[]{
                Events._ID,
                Events.DTSTART,
                Events.RRULE,
            },
            Events._ID + " IN (" +
                TextUtils.join(",", plans) + ")",
            null,
            null);
        if (c != null) {
          if (c.moveToFirst()) {
            while (!c.isAfterLast()) {
              planInfo.put(
                  c.getLong(c.getColumnIndex(Events._ID)),
                  Plan.prettyTimeInfo(
                      context,
                      c.getString(c.getColumnIndex(Events.RRULE)),
                      c.getLong(c.getColumnIndex(Events.DTSTART))));
              c.moveToNext();
            }
          }
          c.close();
        }
      }
    }
    isInitializingPlanInfo = false;
  }

  private long getNextInstance(long planId) {
    long result;
    //we go in three passes in order to prevent calendar provider from having to expand too much instances
    //1) one week 2) one month 3) one year
    long now = System.currentTimeMillis();
    long inOneWeek = now + (7 * 24 * 60 * 60 * 1000L);
    long inOneMonth = now + (31 * 24 * 60 * 60 * 1000L);
    long inOneYear = now + (366 * 24 * 60 * 60 * 1000L);
    long[][] intervals = new long[][] {
        {now, inOneWeek},
        {inOneWeek, inOneMonth},
        {inOneMonth, inOneYear}
    };
    for (long[] interval: intervals) {
      Uri.Builder eventsUriBuilder = CalendarProviderProxy.INSTANCES_URI.buildUpon();
      ContentUris.appendId(eventsUriBuilder, interval[0]);
      ContentUris.appendId(eventsUriBuilder, interval[1]);
      Uri eventsUri = eventsUriBuilder.build();
      Cursor c = context.getContentResolver().query(eventsUri, null,
          String.format(Locale.US, CalendarContractCompat.Instances.EVENT_ID + " = %d",
              planId),
          null,
          null);
      if (c != null) {
        if (c.moveToFirst()) {
          result = c.getLong(c.getColumnIndex(CalendarContractCompat.Instances.BEGIN));
          c.close();
          return result;
        }
        c.close();
      }
    }
    return Long.MAX_VALUE;
  }

  @Override
  public int getColumnIndex(String columnName) {
    int result;
    if (columnName.equals(DatabaseConstants.KEY_PLAN_INFO)) {
      result = getColumnCount();
    } else {
      result = super.getColumnIndex(columnName);
    }
    return result;
  }

  @Override
  public String getString(int columnIndex) {
    if (columnIndex == getColumnCount()) {
      return planInfo.get(getLong(getColumnIndex(DatabaseConstants.KEY_PLANID)));
    }
    return super.getString(columnIndex);
  }

  @Override
  public boolean isNull(int columnIndex) {
    if (columnIndex == getColumnCount()) {
      return getString(columnIndex) == null;
    }
    return super.isNull(columnIndex);
  }

  @Override
  protected int getMappedPosition(int pos) {
    return (!isInitializingPlanInfo && shouldSortByNextInstance) ? sortedPositions.get(pos) : pos;
  }
}
