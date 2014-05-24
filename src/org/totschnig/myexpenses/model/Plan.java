package org.totschnig.myexpenses.model;

import java.io.Serializable;
import java.util.Date;
import java.util.TimeZone;

import org.totschnig.myexpenses.MyApplication;

import com.android.calendar.EventRecurrenceFormatter;
import com.android.calendar.CalendarContractCompat.Events;
import com.android.calendarcommon2.EventRecurrence;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;

/**
 * @author Michael Totschnig
 * holds information about an event in the calendar
 */
public class Plan extends Model implements Serializable {
  public long dtstart;
  public String rrule;
  public String title;
  public String description;

  public Plan(Long id, long dtstart, String rrule, String title, String description) {
    super();
    this.id = id;
    this.dtstart = dtstart;
    this.rrule = rrule;
    this.title = title;
    this.description = description;
  }
  /**
   * insert a new planing event into the calendar
   * @param calendarId
   * @return the id of the created object
   */
  @Override
  public Uri save() {
    String calendarId = MyApplication.getInstance().checkPlanner();
    if (calendarId.equals("-1"))
      return null;
    ContentValues values = new ContentValues();
    values.put(Events.CALENDAR_ID, Long.parseLong(calendarId));
    values.put(Events.TITLE, title);
    values.put(Events.DESCRIPTION, description);
    values.put(Events.DTSTART, dtstart);
    values.put(Events.DTEND, dtstart);
    if (!TextUtils.isEmpty(rrule))
      values.put(Events.RRULE, rrule);
    //values.put(Events.ALL_DAY,1);
    values.put(Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
    return cr().insert(Events.CONTENT_URI, values);
  }

  public static void delete(Long id) {
    String calendarId = MyApplication.getInstance().getSettings()
        .getString(MyApplication.PREFKEY_PLANNER_CALENDAR_ID, "-1");
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
    if (rRule != null) {
      EventRecurrence eventRecurrence = new EventRecurrence();
      eventRecurrence.parse(rRule);
      Time date = new Time();
      date.set(start);
      eventRecurrence.setStartDate(date);
      return EventRecurrenceFormatter.getRepeatString(ctx,ctx.getResources(), eventRecurrence,true);
    } else {
      return java.text.DateFormat
          .getDateInstance(java.text.DateFormat.FULL)
          .format(new Date(start));
    }
  }
}
