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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.provider.TransactionProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.VisibleForTesting;
import timber.log.Timber;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_NUMBERED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAPPED_TEMPLATES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAPPED_TRANSACTIONS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PREDEFINED_METHOD_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_METHODS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TEMPLATES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS;

//TODO refactor to use autovalue
public class PaymentMethod extends Model {
  private String label;
  public static final int EXPENSE = -1;
  public static final int NEUTRAL = 0;
  public static final int INCOME = 1;
  private int paymentType;
  public boolean isNumbered = false;
  private PreDefined preDefined = null;

  public static String[] PROJECTION(Context ctx) {
    return new String[]{
        KEY_ROWID,
        preDefinedName() + " AS " + KEY_PREDEFINED_METHOD_NAME,
        localizedLabelSqlColumn(ctx, KEY_LABEL) + " AS " + KEY_LABEL,
        KEY_TYPE,
        KEY_IS_NUMBERED,
        "(select count(*) from " + TABLE_TRANSACTIONS + " WHERE " + KEY_METHODID + "=" + TABLE_METHODS + "." + KEY_ROWID + ") AS " + KEY_MAPPED_TRANSACTIONS,
        "(select count(*) from " + TABLE_TEMPLATES + " WHERE " + KEY_METHODID + "=" + TABLE_METHODS + "." + KEY_ROWID + ") AS " + KEY_MAPPED_TEMPLATES};
  }

  public static final Uri CONTENT_URI = TransactionProvider.METHODS_URI;
  /**
   * array of account types for which this payment method is applicable
   */
  private ArrayList<AccountType> accountTypes = new ArrayList<>();

  public enum PreDefined {
    CHEQUE(-1, true, R.string.pm_cheque),
    CREDITCARD(-1, false, R.string.pm_creditcard),
    DEPOSIT(1, false, R.string.pm_deposit),
    DIRECTDEBIT(-1, false, R.string.pm_directdebit);

    public final int paymentType;
    public final boolean isNumbered;
    public final int resId;

    PreDefined(int paymentType, boolean isNumbered, int resId) {
      this.isNumbered = isNumbered;
      this.paymentType = paymentType;
      this.resId = resId;
    }

    public String getLocalizedLabel() {
      //TODO use proper context
      return MyApplication.getInstance().getString(resId);
    }
  }

  public static String localizedLabelSqlColumn(Context ctx, String keyLabel) {
    StringBuilder sb = new StringBuilder();
    sb.append("CASE ").append(keyLabel);
    for (PreDefined method : PreDefined.values()) {
      sb.append(" WHEN '").append(method.name()).append("' THEN ");
      DatabaseUtils.appendEscapedSQLString(sb, ctx.getString(method.resId));
    }
    sb.append(" ELSE ").append(keyLabel).append(" END");
    return sb.toString();
  }

  public static String preDefinedName() {
    StringBuilder result = new StringBuilder("CASE " + KEY_LABEL);
    for (PreDefined method : PreDefined.values()) {
      result.append(" WHEN '").append(method.name()).append("' THEN '").append(method.name()).append("'");
    }
    result.append(" ELSE null END");
    return result.toString();

  }

  public static PaymentMethod getInstanceFromDb(long id) {
    PaymentMethod method;
    method = methods.get(id);
    if (method != null) {
      return method;
    }
    Cursor c = cr().query(
        CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(), null, null, null, null);
    if (c == null) {
      return null;
    }
    if (c.getCount() == 0) {
      c.close();
      return null;
    }
    c.moveToFirst();
    method = new PaymentMethod(id);
    method.label = c.getString(c.getColumnIndexOrThrow(KEY_LABEL));
    method.paymentType = c.getInt(c.getColumnIndexOrThrow(KEY_TYPE));
    method.isNumbered = c.getInt(c.getColumnIndexOrThrow(KEY_IS_NUMBERED)) > 0;
    int columnIndexPreDefined = c.getColumnIndexOrThrow(KEY_PREDEFINED_METHOD_NAME);
    if (!c.isNull(columnIndexPreDefined)) {
      method.preDefined = PreDefined.valueOf(
          c.getString(columnIndexPreDefined));
    }
    c.close();
    c = cr().query(TransactionProvider.ACCOUNTTYPES_METHODS_URI,
        new String[]{KEY_TYPE}, KEY_METHODID + " = ?", new String[]{String.valueOf(id)}, null);
    if (c.moveToFirst()) {
      for (int i = 0; i < c.getCount(); i++) {
        try {
          method.addAccountType(AccountType.valueOf(c.getString(c.getColumnIndexOrThrow(KEY_TYPE))));
        } catch (IllegalArgumentException ex) {
          Timber.e(ex, "Found unknown account type in database");
        }
        c.moveToNext();
      }
    }
    c.close();
    methods.put(id, method);
    return method;
  }

  private PaymentMethod(Long id) {
    this.setId(id);
  }

  public PaymentMethod() {
    this("");
  }

  public PaymentMethod(String label) {
    this.paymentType = NEUTRAL;
    this.label = label;
  }

  public int getPaymentType() {
    return paymentType;
  }

  @VisibleForTesting
  public PreDefined getPreDefined() {
    return preDefined;
  }

  public void setPaymentType(int paymentType) {
    this.paymentType = paymentType;
  }

  public void addAccountType(AccountType accountType) {
    if (!accountTypes.contains(accountType))
      accountTypes.add(accountType);
  }

  public void removeAccountType(AccountType accountType) {
    if (accountTypes.contains(accountType))
      accountTypes.remove(accountType);
  }

  public boolean isValidForAccountType(AccountType accountType) {
    return accountTypes.contains(accountType);
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  private static final Map<Long, PaymentMethod> methods = Collections.synchronizedMap(new HashMap<>());

  public Uri save() {
    Uri uri;
    ContentValues initialValues = new ContentValues();
    if (preDefined == null || !preDefined.getLocalizedLabel().equals(label)) {
      initialValues.put(KEY_LABEL, label);
    }
    initialValues.put(KEY_TYPE, paymentType);
    initialValues.put(KEY_IS_NUMBERED, isNumbered);
    if (getId() == 0) {
      uri = cr().insert(CONTENT_URI, initialValues);
      setId(Long.parseLong(uri.getLastPathSegment()));
    } else {
      uri = CONTENT_URI.buildUpon().appendPath(String.valueOf(getId())).build();
      cr().update(uri, initialValues, null, null);
    }
    setMethodAccountTypes();
    if (!methods.containsKey(getId()))
      methods.put(getId(), this);
    return uri;
  }

  private void setMethodAccountTypes() {
    cr().delete(TransactionProvider.ACCOUNTTYPES_METHODS_URI, KEY_METHODID + " = ?", new String[]{String.valueOf(getId())});
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_METHODID, getId());
    for (AccountType accountType : accountTypes) {
      initialValues.put(KEY_TYPE, accountType.name());
      cr().insert(TransactionProvider.ACCOUNTTYPES_METHODS_URI, initialValues);
    }
  }

  /**
   * empty the cache
   */
  public static void clear() {
    methods.clear();
  }

  public static void delete(long id) {
    cr().delete(TransactionProvider.ACCOUNTTYPES_METHODS_URI, KEY_METHODID + " = ?", new String[]{String.valueOf(id)});
    cr().delete(CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(),
        null, null);
  }

  public static int count(String selection, String[] selectionArgs) {
    Cursor mCursor = cr().query(TransactionProvider.ACCOUNTTYPES_METHODS_URI, new String[]{"count(*)"},
        selection, selectionArgs, null);
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

  public static int countPerType(AccountType type) {
    return count("type = ?", new String[]{type.name()});
  }

  /**
   * @param label
   * @return id of new record, or -1, if it already exists
   */
  public static long maybeWrite(String label, AccountType accountType) {
    PaymentMethod paymentMethod = new PaymentMethod(label);
    paymentMethod.addAccountType(accountType);
    Uri uri = paymentMethod.save();
    return uri == null ? -1 : Long.parseLong(uri.getLastPathSegment());
  }

  /**
   * Looks for a method with a label;
   *
   * @param label
   * @return id or -1 if not found
   */
  public static long find(String label) {
    Cursor mCursor = cr().query(CONTENT_URI,
        new String[]{KEY_ROWID}, KEY_LABEL + " = ?", new String[]{label}, null);
    if (mCursor.getCount() == 0) {
      mCursor.close();
      return -1;
    } else {
      mCursor.moveToFirst();
      long result = mCursor.getLong(0);
      mCursor.close();
      return result;
    }
  }
}
