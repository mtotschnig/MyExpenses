package org.totschnig.myexpenses.model;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.provider.TransactionProvider;

import android.content.ContentValues;
import android.net.Uri;

public class Payee extends Model {
  public static final String[] PROJECTION = new String[] {KEY_ROWID, "name"};
  public static final Uri CONTENT_URI = TransactionProvider.PAYEES_URI;
  /**
   * inserts a new payee if it does not exist yet
   * @param name
   * @return id of new record, or -1, if it already exists
   */
  public static long create(String name) {
    ContentValues initialValues = new ContentValues();
    initialValues.put("name", name);
    Uri uri = cr().insert(CONTENT_URI, initialValues);
    if (uri == null)
      return -1;
    return Integer.valueOf(uri.getLastPathSegment());
  }
  public static boolean delete(long id) {
    return cr().delete(CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(),
        null, null) > 0;
  }
}
