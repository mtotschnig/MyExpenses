package org.totschnig.myexpenses.util;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import com.android.calendar.CalendarContractCompat.Events;

import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.provider.DatabaseConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;

public class PlanInfoCursorWrapper extends CursorWrapperHelper {
  private Context context;
  private final HashMap<Long, String> planInfo = new HashMap<>();
  private final HashMap<Integer, Long> nextInstance = new HashMap<>();
  private ArrayList<Integer> sortedPositions = new ArrayList<>();
  private boolean doSort;

  public PlanInfoCursorWrapper(Context context, Cursor cursor) {
    super(cursor);
    this.context = context;
  }

  public void fetchPlanInfo() {
    doSort = false;
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
              return -1;
            }
          }
          if (rhNextInstance == null) {
            return 1;
          }
          return lhNextInstance.compareTo(rhNextInstance);
        }
      });
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
    doSort = true;
  }

  private long getNextInstance(long planId) {
    return planId; // TODO fetch from calendar;
  }

  @Override
  public int getColumnIndex(String columnName) {
    int result;
    if (columnName.equals(DatabaseConstants.KEY_PLAN_INFO)) {
      result = getColumnCount();
    } else {
      result = super.getColumnIndex(columnName);
    }
    Log.d("PlanInfoCursorWrapper", columnName + " : " + result);
    return result;
  }

  @Override
  public String getString(int columnIndex) {
    Log.d("PlanInfoCursorWrapper", "" + columnIndex);
    if (columnIndex == getColumnCount()) {
      return planInfo.get(getLong(getColumnIndex(DatabaseConstants.KEY_PLANID)));
    }
    return super.getString(columnIndex);
  }

  @Override
  protected int getMappedPosition(int pos) {
    return doSort ? sortedPositions.get(pos) : pos;
  }
}
