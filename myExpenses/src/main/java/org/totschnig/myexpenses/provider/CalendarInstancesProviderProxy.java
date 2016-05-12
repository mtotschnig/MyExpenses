package org.totschnig.myexpenses.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.android.calendar.CalendarContractCompat;

import org.totschnig.myexpenses.BuildConfig;

/**
 * Proxy for {@link com.android.calendar.CalendarContractCompat.Instances} which allows to swap in
 * alternate implementation in context where the Instances table does not work, e.g. Blackberry
 */
public class CalendarInstancesProviderProxy extends ContentProvider {
  public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".calendarinstances";
  public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/instances/when");

  @Override
  public boolean onCreate() {
    return true;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                      String sortOrder) {
    Uri proxiedUri = Uri.parse(uri.toString().replace(
        CONTENT_URI.toString(), CalendarContractCompat.Instances.CONTENT_URI.toString()));
    return getContext().getContentResolver().query(proxiedUri, projection, selection, selectionArgs,
        sortOrder);
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
