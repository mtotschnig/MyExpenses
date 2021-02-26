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

import java.util.ArrayList;
import java.util.Collections;

import androidx.collection.LongSparseArray;
import androidx.collection.SparseArrayCompat;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLANID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLAN_INFO;

public class PlanInfoCursorWrapper extends CursorWrapperHelper {
  private final Context context;
  private final LongSparseArray<String> planInfo = new LongSparseArray<>();
  private final SparseArrayCompat<Long> nextInstance = new SparseArrayCompat<>();
  private final ArrayList<Integer> sortedPositions = new ArrayList<>();
  private final boolean shouldSortByNextInstance;

  public PlanInfoCursorWrapper(Context context, Cursor cursor, boolean shouldSortByNextInstance, boolean initializePlanInfo) {
    super(cursor);
    this.context = context;
    if (initializePlanInfo) {
      this.shouldSortByNextInstance = shouldSortByNextInstance;
      initializePlanInfo();
    } else {
      this.shouldSortByNextInstance = false;
    }
  }

  private void initializePlanInfo() {
    Cursor wrapped = getWrappedCursor();
    if (wrapped.moveToFirst()) {
      ArrayList<Long> plans = new ArrayList<>();
      long planId;
      int columnIndexPlanId = getColumnIndex(KEY_PLANID);
      while (!wrapped.isAfterLast()) {
        int wrappedPos = wrapped.getPosition();
        if ((planId = getLong(columnIndexPlanId)) != 0L) {
          plans.add(planId);
          if (shouldSortByNextInstance) {
            nextInstance.put(wrappedPos, getNextInstance(planId));
          }
        }
        sortedPositions.add(wrappedPos);
        wrapped.moveToNext();
      }
      if (shouldSortByNextInstance) {
        Collections.sort(sortedPositions, (lhs, rhs) -> {
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
        });
      }
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
  }

  private long getNextInstance(long planId) {
    long result;
    //we go in three passes in order to prevent calendar provider from having to expand too much instances
    //1) one week 2) one month 3) one year
    long now = System.currentTimeMillis();
    long inOneWeek = now + (7 * 24 * 60 * 60 * 1000);
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
          CalendarContractCompat.Instances.EVENT_ID + " = ?",
          new String[]{String.valueOf(planId)},
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
    if (columnName.equals(KEY_PLAN_INFO)) {
      result = getColumnCount();
    } else {
      result = super.getColumnIndex(columnName);
    }
    return result;
  }

  @Override
  public String getColumnName(int columnIndex) {
    if (columnIndex == getColumnCount()) {
      return KEY_PLAN_INFO;
    }
    return super.getColumnName(columnIndex);
  }

  @Override
  public String getString(int columnIndex) {
    if (columnIndex == getColumnCount()) {
      return planInfo.get(getLong(getColumnIndex(KEY_PLANID)));
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
    return (shouldSortByNextInstance) ? sortedPositions.get(pos) : pos;
  }
}
