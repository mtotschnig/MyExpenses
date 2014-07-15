/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.totschnig.myexpenses.model;

import java.util.Locale;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.provider.TransactionProvider;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class ContribFeature extends Model {

  public enum Feature {
    ACCOUNTS_UNLIMITED(false),PLANS_UNLIMITED(false),RESET_ALL,SECURITY_QUESTION,SPLIT_TRANSACTION,DISTRIBUTION, TEMPLATE_WIDGET;

    private Feature() {
      this(true);
    }
    private Feature(boolean hasTrial) {
      this.hasTrial = hasTrial;
    }
    public boolean hasTrial;
    public static final Uri CONTENT_URI = TransactionProvider.FEATURE_USED_URI;    /**
     * how many times contrib features can be used for free
     */
    public static int USAGES_LIMIT = 5;
    public String toString() {
      return name().toLowerCase(Locale.US);
    }
    public int countUsages() {
      int result = 0;
      Cursor mCursor = cr().query(CONTENT_URI,new String[] {"count(*)"},
          "feature = ?", new String[] {toString()}, null);
      if (mCursor != null) {
        if (mCursor.moveToFirst()) {
          result = mCursor.getInt(0);
        }
        mCursor.close();
      }
      return result;
    }
    public void recordUsage() {
      if (!MyApplication.getInstance().isContribEnabled()) {
        //TODO strict mode background task
        ContentValues initialValues = new ContentValues();
        initialValues.put("feature", toString());
        cr().insert(CONTENT_URI, initialValues);
      }
    }
    public int usagesLeft() {
      return hasTrial ? USAGES_LIMIT - countUsages() : 0;
    }
  }

  @Override
  public Uri save() {
    // TODO Auto-generated method stub
    return null;
  }
}