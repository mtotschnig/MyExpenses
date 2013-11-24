package org.totschnig.myexpenses.model;

import java.io.Serializable;
import java.util.TimeZone;

import org.totschnig.myexpenses.MyApplication;

import com.android.calendar.CalendarContractCompat.Events;

import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;

/**
 * @author Michael Totschnig
 * holds information about an event in the calendar
 */
public class Plan extends Model implements Serializable {
  public long id;
  public long dtstart;
  public long dtend;
  public String rrule;
  public String title;
  public Plan(long id, long dtstart, long dtend, String rrule, String title) {
    super();
    this.id = id;
    this.dtstart = dtstart;
    this.dtend = dtend;
    this.rrule = rrule;
    this.title = title;
  }
  @Override
  public Uri save() {
    // not handled here, but in Calendar app
    return null;
  }
  public static void delete(Long id) {
    cr().delete(
        Events.CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(),
        null,
        null);
  }
  /**
   * insert a new planing event into the calendar
   * @param calendarId
   * @return the id of the created objcet
   */
  public static Long create(String title) {
    String calendarId = MyApplication.getInstance().requirePlaner();
    if (calendarId.equals("-1"))
      return null;
    long now = System.currentTimeMillis();
    ContentValues values = new ContentValues();
    values.put(Events.CALENDAR_ID, Long.parseLong(calendarId));
    values.put(Events.TITLE, title);
    values.put(Events.DTSTART, now);
    values.put(Events.DTEND, now);
    values.put(Events.ALL_DAY,1);
    values.put(Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
    Uri uri = cr().insert(Events.CONTENT_URI, values);
    return ContentUris.parseId(uri);
  }
}
