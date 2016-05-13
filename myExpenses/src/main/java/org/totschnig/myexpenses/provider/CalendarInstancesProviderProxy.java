package org.totschnig.myexpenses.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import com.android.calendar.CalendarContractCompat;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.util.Utils;

import java.util.Calendar;
import java.util.TimeZone;

import hirondelle.date4j.DateTime;

/**
 * Proxy for {@link com.android.calendar.CalendarContractCompat.Instances} which allows to swap in
 * alternate implementation in context where the Instances table does not work, e.g. Blackberry
 */
public class CalendarInstancesProviderProxy extends ContentProvider {
  public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".calendarinstances";
  public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/instances/when");
  private static final String[] INSTANCE_PROJECTION = new String[]{
      CalendarContractCompat.Instances.EVENT_ID,
      CalendarContractCompat.Instances._ID,
      CalendarContractCompat.Instances.BEGIN
  };

  private static final UriMatcher URI_MATCHER;

  private static final int INSTANCES_ANDROID = 1;

  private static final int INSTANCES_BLACKBERRY_DAY = 2;

  private static final int INSTANCES_BLACKBERRY_MONTH = 3;

  static {
    URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    URI_MATCHER.addURI(AUTHORITY, "instances/when/#/#", INSTANCES_ANDROID);
  }

  @Override
  public boolean onCreate() {
    return true;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                      String sortOrder) {
    if (projection != null) {
      throw new IllegalStateException("Must pass in null projection");
    }
    int uriMatch = URI_MATCHER.match(uri);
    if (uriMatch != INSTANCES_ANDROID) {
      throw new IllegalArgumentException("Unknown URL " + uri);
    }
    if (Utils.IS_ANDROID) {
      //Instances.Content_URI returns events that fall totally or partially in a given range
      //we additionally select only instances where the begin is inside the range
      //because we want to deal with each instance only once
      //the calendar content provider on Android < 4 does not interpret the selection arguments
      //hence we put them into the selection
      long begin = Long.parseLong(uri.getPathSegments().get(2));
      long end = Long.parseLong(uri.getPathSegments().get(3));
      selection = selection == null ? "" : (selection + " AND ");
      selection += CalendarContractCompat.Instances.BEGIN +
          " BETWEEN " + begin + " AND " + end;
      Uri proxiedUri = Uri.parse(uri.toString().replace(
          CONTENT_URI.toString(), CalendarContractCompat.Instances.CONTENT_URI.toString()));
      return getContext().getContentResolver().query(proxiedUri, INSTANCE_PROJECTION, selection, selectionArgs,
          sortOrder);
    }

    MatrixCursor result = new MatrixCursor(INSTANCE_PROJECTION);
    String eventSelection = selection.replace(CalendarContractCompat.Instances.EVENT_ID,
        CalendarContractCompat.Events._ID);
    String[] eventProjection = new String[]{
        CalendarContractCompat.Events._ID,
        CalendarContractCompat.Events.DTSTART,
        CalendarContractCompat.Events.RRULE};
    Cursor eventcursor = getContext().getContentResolver().query(CalendarContractCompat.Events.CONTENT_URI,
        eventProjection, eventSelection, selectionArgs, sortOrder);
    DateTime start = DateTime.forInstant(Long.parseLong(uri.getPathSegments().get(2)), TimeZone.getDefault());
    DateTime end = DateTime.forInstant(Long.parseLong(uri.getPathSegments().get(3)), TimeZone.getDefault());
    if (eventcursor != null) {
      if (eventcursor.moveToFirst()) {
        while (!eventcursor.isAfterLast()) {
          String eventId = eventcursor.getString(0);
          long dtstart = eventcursor.getLong(1);
          String rrule = eventcursor.getString(2);
          for (DateTime dayToCheck = start; dayToCheck.lteq(end); dayToCheck = dayToCheck.plusDays(1)) {
            if (isInstanceOfPlan(dayToCheck, dtstart, rrule)) {
              result.addRow(new String[]{
                  eventId,
                  String.valueOf(dayToCheck.getYear() * 1000 + dayToCheck.getDayOfYear()),
                  String.valueOf(dayToCheck.getMilliseconds(TimeZone.getDefault())),
              });
            }
          }
          eventcursor.moveToNext();
        }
      }
      eventcursor.close();
    }
    return result;
  }

  private boolean isInstanceOfPlan(DateTime dayToCheck, long dtstart, String rrule) {
    return dayToCheck.getDay() % 3 == 0;
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
