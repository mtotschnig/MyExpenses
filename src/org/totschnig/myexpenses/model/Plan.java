package org.totschnig.myexpenses.model;

import java.io.Serializable;
import java.util.TimeZone;

import org.totschnig.myexpenses.MyApplication;
import com.android.calendar.CalendarContractCompat.Events;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * @author Michael Totschnig
 * holds information about an event in the calendar
 */
public class Plan extends Model implements Serializable {
  public long id;
  public long dtstart;
  public String rrule;
  public String title;
  public String description;
  public Plan(long id, long dtstart, String rrule, String title, String description) {
    super();
    this.id = id;
    this.dtstart = dtstart;
    this.rrule = rrule;
    this.title = title;
    this.description = description;
  }
  @Override
  public Uri save() {
    // not handled here, but in Calendar app
    return null;
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
  /**
   * insert a new planing event into the calendar
   * @param calendarId
   * @return the id of the created object
   */
  public static Long create(Plan plan) {
    String calendarId = MyApplication.getInstance().checkPlanner();
    if (calendarId.equals("-1"))
      return null;
    ContentValues values = new ContentValues();
    values.put(Events.CALENDAR_ID, Long.parseLong(calendarId));
    values.put(Events.TITLE, plan.title);
    values.put(Events.DESCRIPTION, plan.description);
    values.put(Events.DTSTART, plan.dtstart);
    values.put(Events.DTEND, plan.dtstart);
    //values.put(Events.ALL_DAY,1);
    values.put(Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
    Uri uri = cr().insert(Events.CONTENT_URI, values);
    return ContentUris.parseId(uri);
  }
}
