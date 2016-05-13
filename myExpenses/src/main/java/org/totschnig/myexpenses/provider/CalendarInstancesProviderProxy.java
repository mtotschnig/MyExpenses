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

/**
 * Proxy for {@link com.android.calendar.CalendarContractCompat.Instances} which allows to swap in
 * alternate implementation in context where the Instances table does not work, e.g. Blackberry
 */
public class CalendarInstancesProviderProxy extends ContentProvider {
  public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".calendarinstances";
  public static final Uri CONTENT_URI_ANDROID = Uri.parse("content://" + AUTHORITY + "/instances/when");
  public static final Uri CONTENT_URI_BLACKBERY_MONTH = Uri.parse("content://" + AUTHORITY + "/instances/month");
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
    URI_MATCHER.addURI(AUTHORITY, "instances/day/#/#", INSTANCES_BLACKBERRY_DAY);
    URI_MATCHER.addURI(AUTHORITY, "instances/month/#/#", INSTANCES_BLACKBERRY_MONTH);
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
    switch (uriMatch) {
      case INSTANCES_ANDROID:
        if (!Utils.IS_ANDROID) {
          throw new IllegalStateException("This query can only be used on Android");
        }
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
            CONTENT_URI_ANDROID.toString(), CalendarContractCompat.Instances.CONTENT_URI.toString()));
        return getContext().getContentResolver().query(proxiedUri, INSTANCE_PROJECTION, selection, selectionArgs,
            sortOrder);

      //case INSTANCES_BLACKBERRY_DAY:
      case INSTANCES_BLACKBERRY_MONTH:
        MatrixCursor result = new MatrixCursor(INSTANCE_PROJECTION);
        String eventSelection = selection.replace(CalendarContractCompat.Instances.EVENT_ID,
            CalendarContractCompat.Events._ID);
        String[] eventProjection = new String[]{
            CalendarContractCompat.Events._ID,
            CalendarContractCompat.Events.DTSTART,
            CalendarContractCompat.Events.RRULE};
        Cursor eventcursor = getContext().getContentResolver().query(CalendarContractCompat.Events.CONTENT_URI,
            eventProjection, eventSelection, selectionArgs, sortOrder);
        int year = Integer.parseInt(uri.getPathSegments().get(2));
        int month = Integer.parseInt(uri.getPathSegments().get(3));
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.DAY_OF_MONTH, 15);
        String instanceId = String.valueOf(year * 1000 + cal.get(Calendar.DAY_OF_YEAR));
        if (eventcursor != null) {
          if (eventcursor.moveToFirst()) {
            while (!eventcursor.isAfterLast()) {
              result.addRow(new String[]{
                  eventcursor.getString(0),
                  instanceId,
                  String.valueOf(cal.getTimeInMillis()),
              });
              eventcursor.moveToNext();
            }
          }
          eventcursor.close();
        }
        return result;
    }
    throw new IllegalArgumentException("Unknown URL " + uri);
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
