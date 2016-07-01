package org.totschnig.myexpenses.model;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;

import com.android.calendar.CalendarContractCompat.Events;
import com.android.calendar.EventRecurrenceFormatter;
import com.android.calendarcommon2.EventRecurrence;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.AcraHelper;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author Michael Totschnig
 * holds information about an event in the calendar
 */
public class Plan extends Model implements Serializable {
  public long dtstart;
  public String rrule;
  public String title;
  public String description;

  public enum Recurrence {
    NONE, ONETIME, DAILY, WEEKLY, MONTHLY, YEARLY;

    public String toRrule() {
      switch (this) {
        case DAILY:
          return "FREQ=DAILY";
        case WEEKLY:
          return "FREQ=WEEKLY";
        case MONTHLY:
          return "FREQ=MONTHLY";
        case YEARLY:
          return "FREQ=YEARLY";
        default:
          return null;
      }
    }

    public String getLabel(Context context) {
      switch (this) {
        case NONE:
          return "- - - -";
        case ONETIME:
          return context.getString(R.string.does_not_repeat);
        case DAILY:
          return context.getString(R.string.daily_plain);
        case WEEKLY:
          return context.getString(R.string.weekly_plain);
        case MONTHLY:
          return context.getString(R.string.monthly);
        case YEARLY:
          return context.getString(R.string.yearly_plain);
      }
      return null;
    }

    public static Plan.Recurrence[] valuesWithoutOneTime() {
      return new Plan.Recurrence[] {
        NONE, DAILY, WEEKLY, MONTHLY, YEARLY
      };
    }
  }

  private Plan(Long id, long dtstart, String rrule, String title, String description) {
    this.setId(id);
    this.dtstart = dtstart;
    this.rrule = rrule;
    this.title = title;
    this.description = description;
  }

  public Plan(Calendar cal, String rrule, String title, String description) {
    this.dtstart = cal.getTimeInMillis();
    this.rrule = rrule;
    this.title = title;
    this.description = description;

  }

  public static Plan getInstanceFromDb(long planId) {
    Plan plan = null;
    Cursor c = cr().query(
        ContentUris.withAppendedId(Events.CONTENT_URI, planId),
        new String[]{
            Events._ID,
            Events.DTSTART,
            Events.RRULE,
            Events.TITLE},
        null,
        null,
        null);
    if (c != null) {
      if (c.moveToFirst()) {
        long eventId = c.getLong(c.getColumnIndexOrThrow(Events._ID));
        long dtStart = c.getLong(c.getColumnIndexOrThrow(Events.DTSTART));
        String rRule = c.getString(c.getColumnIndexOrThrow(Events.RRULE));
        String title = c.getString(c.getColumnIndexOrThrow(Events.TITLE));
        plan = new Plan(
            eventId,
            dtStart,
            rRule,
            title,
            "" // we do not need the description stored in the event
        );
        c.close();
      }
    }
    return plan;
  }

  /**
   * insert a new planing event into the calendar
   * @return the id of the created object
   */
  @Override
  public Uri save() {
    Uri uri;
    ContentValues values = new ContentValues();
    values.put(Events.TITLE, title);
    values.put(Events.DESCRIPTION, description);
    if (!TextUtils.isEmpty(rrule)) {
      values.put(Events.RRULE, rrule);
    }
    values.put(Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
    if (getId() == 0) {
      String calendarId = MyApplication.getInstance().checkPlanner();
      if (calendarId.equals(MyApplication.INVALID_CALENDAR_ID)) {
        calendarId = MyApplication.getInstance().createPlanner(true);
        if (calendarId.equals(MyApplication.INVALID_CALENDAR_ID)) {
          throw new CalendarIntegrationNotAvailableException();
        }
      }
      values.put(Events.CALENDAR_ID, Long.parseLong(calendarId));
      values.put(Events.DTSTART, dtstart);
      values.put(Events.DTEND, dtstart);
      uri = cr().insert(Events.CONTENT_URI, values);
      setId(ContentUris.parseId(uri));
    } else {
      uri = ContentUris.withAppendedId(Events.CONTENT_URI, getId());
      cr().update(uri, values, null, null);
    }
    return uri;
  }


  public static void delete(Long id) {
    String calendarId = PrefKey.PLANNER_CALENDAR_ID.getString("-1");
    Uri eventUri = Events.CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
    Cursor eventCursor = cr().query(
        eventUri,
        new String[]{"1 as ignore"},
        Events.CALENDAR_ID + " = ?",
        new String[] {calendarId},
        null);
    if (eventCursor != null && eventCursor.getCount()>0) {
      cr().delete(
          eventUri,
          null,
          null);
    } else {
      Log.w(MyApplication.TAG,
          String.format(
              "Attempt to delete event %d, which does not exist in calendar %s, has been blocked",
              id,calendarId));
    }
    eventCursor.close();
  }

  public static String prettyTimeInfo(Context ctx, String rRule, Long start) {
    if (!TextUtils.isEmpty(rRule)) {
      EventRecurrence eventRecurrence = new EventRecurrence();
      try {
        eventRecurrence.parse(rRule);
      } catch (EventRecurrence.InvalidFormatException e) {
        AcraHelper.report(e,"rRule",rRule);
        return e.getMessage();
      }
      Time date = new Time();
      date.set(start);
      eventRecurrence.setStartDate(date);
      return EventRecurrenceFormatter.getRepeatString(ctx,ctx.getResources(), eventRecurrence, true);
    } else {
      return java.text.DateFormat
          .getDateInstance(java.text.DateFormat.FULL)
          .format(new Date(start));
    }
  }

  public void updateCustomAppUri(String customAppUri) {
    if (getId()==0) {
      throw new IllegalStateException("Can not set custom app uri on unsaved plan");
    }
    updateCustomAppUri(getId(), customAppUri);
  }

  public static void updateCustomAppUri(Long id, String customAppUri) {
    if (android.os.Build.VERSION.SDK_INT >= 16) {
      try {
        ContentValues values = new ContentValues();
        values.put(Events.CUSTOM_APP_URI, customAppUri);
        values.put(Events.CUSTOM_APP_PACKAGE, MyApplication.getInstance().getPackageName());
        cr().update(ContentUris.withAppendedId(Events.CONTENT_URI, id), values, null, null);
      } catch (SQLiteException e) {
        // we have seen a bugy calendar provider implementation on Symphony phone
      }
    }
  }

  public static class CalendarIntegrationNotAvailableException extends IllegalStateException {
  }
}
