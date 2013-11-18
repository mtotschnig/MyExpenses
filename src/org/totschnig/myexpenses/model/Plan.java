package org.totschnig.myexpenses.model;

import java.io.Serializable;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.provider.CalendarContract.Events;

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
  @SuppressLint("NewApi")
  public static void delete(Long id) {
    cr().delete(
        Events.CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(),
        null,
        null);
  }
}
