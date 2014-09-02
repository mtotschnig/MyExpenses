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

import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

import java.util.ArrayList;
import java.util.HashMap;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Account.Type;
import org.totschnig.myexpenses.provider.TransactionProvider;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class PaymentMethod extends Model {
  private String label;
  public static final int EXPENSE =  -1;
  public static final int NEUTRAL = 0;
  public static final int INCOME = 1;
  private int paymentType;
  public boolean isNumbered = false;
  public static final String[] PROJECTION = new String[] {
    KEY_ROWID,
    KEY_LABEL,
    KEY_TYPE,
    KEY_IS_NUMBERED,
    "(select count(*) from " + TABLE_TRANSACTIONS + " WHERE " + KEY_METHODID + "=" + TABLE_METHODS + "." + KEY_ROWID + ") AS " + KEY_MAPPED_TRANSACTIONS,
    "(select count(*) from " + TABLE_TEMPLATES    + " WHERE " + KEY_METHODID + "=" + TABLE_METHODS + "." + KEY_ROWID + ") AS " + KEY_MAPPED_TEMPLATES};
  public static final Uri CONTENT_URI = TransactionProvider.METHODS_URI;
  /**
   * array of account types for which this payment method is applicable
   */
  private ArrayList<Account.Type> accountTypes = new ArrayList<Account.Type>();
  
  public enum PreDefined {
    CHEQUE(-1,true),CREDITCARD(-1),DEPOSIT(1),DIRECTDEBIT(-1);
    public final int paymentType;
    public final boolean isNumbered;
    PreDefined(int paymentType, boolean isNumbered) {
      this.isNumbered = isNumbered;
      this.paymentType = paymentType;
    }
    PreDefined(int paymentType) {
      this(paymentType,false);
    }
  }
  public static PaymentMethod getInstanceFromDb(long id) {
    PaymentMethod method;
    method = methods.get(id);
    if (method != null) {
      return method;
    }
    Cursor c = cr().query(
        CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(), null,null,null, null);
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
    c.close();
    c = cr().query(TransactionProvider.ACCOUNTTYPES_METHODS_URI,
        new String[] {KEY_TYPE}, KEY_METHODID + " = ?", new String[] {String.valueOf(id)}, null);
    if(c.moveToFirst()) {
      for (int i = 0; i < c.getCount(); i++){
        try {
          method.addAccountType(Account.Type.valueOf(c.getString(c.getColumnIndexOrThrow(KEY_TYPE))));
        } catch (IllegalArgumentException ex) { 
          Log.w("MyExpenses","Found unknown account type in database");
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
    this.paymentType = NEUTRAL;
    this.label = "";
  }
  public int getPaymentType() {
    return paymentType;
  }
  public void setPaymentType(int paymentType) {
    this.paymentType = paymentType;
  }
  public void addAccountType(Account.Type accountType) {
    if (!accountTypes.contains(accountType))
      accountTypes.add(accountType);
  }
  public void removeAccountType(Account.Type accountType) {
    if (accountTypes.contains(accountType))
      accountTypes.remove(accountType);
  }
  public boolean isValidForAccountType(Account.Type accountType) {
    return accountTypes.contains(accountType);
  }
  
  public String getLabel() {
    return label;
  }
  public void setLabel(String label) {
    this.label = label;
  }
  //TODO we should not need to instantiate the methods
  //to get their display label
  //move all calls from the instance method to the static metod
  public String getDisplayLabel() {
    return PaymentMethod.getDisplayLabel(label);
  }
  public static String getDisplayLabel(String label) {
    Context ctx = MyApplication.getInstance();
    if (label.equals("CHEQUE"))
      return ctx.getString(R.string.pm_cheque);
    if (label.equals("CREDITCARD"))
      return ctx.getString(R.string.pm_creditcard);
    if (label.equals("DEPOSIT"))
      return ctx.getString(R.string.pm_deposit);
    if (label.equals("DIRECTDEBIT"))
      return ctx.getString(R.string.pm_directdebit);
    return label;
  }
  static HashMap<Long,PaymentMethod> methods = new HashMap<Long,PaymentMethod>();
  
  public Uri save() {
    Uri uri;
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_LABEL, label);
    initialValues.put(KEY_TYPE,paymentType);
    initialValues.put(KEY_IS_NUMBERED,isNumbered);
    if (getId() == 0) {
      uri = cr().insert(CONTENT_URI, initialValues);
      setId(Long.valueOf(uri.getLastPathSegment()));
    } else {
      uri = CONTENT_URI.buildUpon().appendPath(String.valueOf(getId())).build();
      cr().update(uri,initialValues,null,null);
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
    for (Account.Type accountType : accountTypes) {
      initialValues.put(KEY_TYPE,accountType.name());
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
    cr().delete(TransactionProvider.ACCOUNTTYPES_METHODS_URI,KEY_METHODID + " = ?",new String[] {String.valueOf(id)});
    cr().delete(CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(),
        null, null);
  }
  public static int count(String selection,String[] selectionArgs) {
    Cursor mCursor = cr().query(TransactionProvider.ACCOUNTTYPES_METHODS_URI,new String[] {"count(*)"},
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
  public static int countPerType(Type type) {
    return count("type = ?", new String[] {type.name()});
  }
  /**
   * Looks for a method with a label; currently only used from test
   * @param label
   * @return id or -1 if not found
   */
  public static long find(String label) {
    Cursor mCursor = cr().query(CONTENT_URI,
        new String[] {KEY_ROWID}, KEY_LABEL + " = ?", new String[]{label}, null);
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
