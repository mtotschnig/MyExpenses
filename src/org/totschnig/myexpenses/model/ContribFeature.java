package org.totschnig.myexpenses.model;

import java.util.Locale;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.provider.TransactionProvider;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class ContribFeature extends Model {

  public enum Feature {
    EDIT_TEMPLATE,RESTORE,AGGREGATE,RESET_ALL,SECURITY_QUESTION,CLONE_TRANSACTION;
    
    public static final Uri CONTENT_URI = TransactionProvider.FEATURE_USED_URI;    /**
     * how many times contrib features can be used for free
     */
    public static int USAGES_LIMIT = 5;
    public String toString() {
      return name().toLowerCase(Locale.US);
    }
    public int countUsages() {
      Cursor mCursor = cr().query(CONTENT_URI,new String[] {"count(*)"},
          "feature = ?", new String[] {toString()}, null);
      if (mCursor.getCount() == 0) {
        mCursor.close();
        return 0;
      } else {
        mCursor.moveToFirst();
        int result = mCursor.getInt(0);
        mCursor.close();
        return result;
      }
    }
    public void recordUsage() {
      if (!MyApplication.getInstance().isContribEnabled) {
        ContentValues initialValues = new ContentValues();
        initialValues.put("feature", toString());
        cr().insert(CONTENT_URI, initialValues);
      }
    }
    public int usagesLeft() {
      return USAGES_LIMIT - countUsages();
    }
  }
}