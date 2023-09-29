package org.totschnig.myexpenses.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import androidx.annotation.NonNull;

import org.totschnig.myexpenses.calendar.EventRecurrence;

import org.totschnig.myexpenses.BuildConfig;

import java.util.TimeZone;

import hirondelle.date4j.DateTime;

/**
 * Proxy for {@link  CalendarContract.Instances} which allows to swap in
 * alternate implementation in context where the Instances table does not work, e.g. Blackberry
 */
public class CalendarProviderProxy extends ContentProvider {
  public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".calendarinstances";
  public static final Uri INSTANCES_URI = Uri.parse("content://" + AUTHORITY + "/instances/when");
  private static final String[] INSTANCE_PROJECTION = new String[]{
      CalendarContract.Instances.EVENT_ID,
      CalendarContract.Instances.BEGIN,
      CalendarContract.Instances.TITLE
  };

  private static final UriMatcher URI_MATCHER;

  private static final int INSTANCES_WHEN = 1;
  private static final int EVENTS = 2; //currently not used, but possibly in the future we provide
  //an implementation that does not need the platform calendar

  static {
    URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    URI_MATCHER.addURI(AUTHORITY, "instances/when/*/*", INSTANCES_WHEN);
    URI_MATCHER.addURI(AUTHORITY, "events", EVENTS);
  }

  @Override
  public boolean onCreate() {
    return true;
  }

  @Override
  public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs,
                      String sortOrder) {
    int uriMatch = URI_MATCHER.match(uri);
    switch (uriMatch) {
      case INSTANCES_WHEN:
        if (projection != null) {
          throw new IllegalStateException("Must pass in null projection");
        }
        long startMilliseconds = Long.parseLong(uri.getPathSegments().get(2));
        long endMilliseconds = Long.parseLong(uri.getPathSegments().get(3));
        //Instances.Content_URI returns events that fall totally or partially in a given range
        //we additionally select only instances where the begin is inside the range
        //because we want to deal with each instance only once
        //the calendar content provider on Android < 4 does not interpret the selection arguments
        //hence we put them into the selection
        selection = selection == null ? "" : (selection + " AND ");
        selection += CalendarContract.Instances.BEGIN +
            " BETWEEN " + startMilliseconds + " AND " + endMilliseconds;
        Uri proxiedUri = Uri.parse(uri.toString().replace(
            INSTANCES_URI.toString(), CalendarContract.Instances.CONTENT_URI.toString()));
        return getContext().getContentResolver().query(proxiedUri, INSTANCE_PROJECTION, selection, selectionArgs,
            sortOrder);

      case EVENTS:
        return getContext().getContentResolver().query(CalendarContract.Events.CONTENT_URI,
            projection, selection, selectionArgs, sortOrder);
      default:
        throw new IllegalArgumentException("Unknown URL " + uri);
    }
  }

  private boolean isInstanceOfPlan(DateTime dayToCheck, long dtstart, EventRecurrence recurrence) {
    DateTime startDate = DateTime.forInstant(dtstart, TimeZone.getDefault());
    if (recurrence == null) {
      return dayToCheck.isSameDayAs(startDate);
    }
    switch (recurrence.freq) {
      case EventRecurrence.DAILY:
        return true;
      case EventRecurrence.WEEKLY:
        return dayToCheck.getWeekDay().equals(startDate.getWeekDay());
      case EventRecurrence.MONTHLY:
        return dayToCheck.getDay().equals(startDate.getDay());
      case EventRecurrence.YEARLY:
        return dayToCheck.getDay().equals(startDate.getDay()) &&
            dayToCheck.getMonth().equals(startDate.getMonth());
    }
    throw new IllegalStateException("Unhandled event recurrence" + recurrence);
  }

  public static long calculateId(long date) {
    return calculateId(DateTime.forInstant(date, TimeZone.getTimeZone("UTC")));
  }

  public static long calculateId(DateTime dateTime) {
    return dateTime.getYear() * 1000 + dateTime.getDayOfYear();
  }

  @Override
  public String getType(Uri uri) {
    return null;
  }

  /**
   * not implemented
   */
  @Override
  public Uri insert(Uri uri, ContentValues values) {
    return null;
  }

  /**
   * not implemented
   */
  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    return 0;
  }

  /**
   * not implemented
   */
  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    return 0;
  }
}
