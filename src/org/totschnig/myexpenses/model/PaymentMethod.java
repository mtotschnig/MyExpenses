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

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

import java.util.ArrayList;
import java.util.HashMap;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.R.string;
import org.totschnig.myexpenses.model.Account.Type;
import org.totschnig.myexpenses.provider.TransactionProvider;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.util.Log;

public class PaymentMethod {
  public long id;
  private String label;
  public static final int EXPENSE =  -1;
  public static final int NEUTRAL = 0;
  public static final int INCOME = 1;
  private int paymentType;
  public static final String[] PROJECTION = new String[] {KEY_ROWID,"label"};
  public static final Uri CONTENT_URI = TransactionProvider.METHODS_URI;
  /**
   * array of account types for which this payment method is applicable
   */
  private ArrayList<Account.Type> accountTypes = new ArrayList<Account.Type>();
  public PreDefined predef;
  
  public enum PreDefined {
    CHEQUE(-1),CREDITCARD(-1),DEPOSIT(1),DIRECTDEBIT(-1);
    public final int paymentType;
    PreDefined(int paymentType) {
      this.paymentType = paymentType;
    }
  }
  private PaymentMethod(long id) throws DataObjectNotFoundException {
    this.id = id;
    String[] projection = new String[] {"label","type"};
    Cursor c = MyApplication.cr().query(
        CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(), projection,null,null, null);
    if (c == null || c.getCount() == 0) {
      throw new DataObjectNotFoundException();
    }
    c.moveToFirst();

    this.label = c.getString(c.getColumnIndexOrThrow("label"));
    this.paymentType = c.getInt(c.getColumnIndexOrThrow("type"));
    c.close();
    try {
      predef = PreDefined.valueOf(this.label);
    } catch (IllegalArgumentException ex) { 
      predef = null;
    }
    c = MyApplication.cr().query(TransactionProvider.ACCOUNTTYPES_METHODS_URI,
        new String[] {"type"}, "method_id = ?", new String[] {String.valueOf(id)}, null);
    if(c.moveToFirst()) {
      for (int i = 0; i < c.getCount(); i++){
        try {
          addAccountType(Account.Type.valueOf(c.getString(c.getColumnIndexOrThrow("type"))));
        } catch (IllegalArgumentException ex) { 
          Log.w("MyExpenses","Found unknown account type in database");
        }
        c.moveToNext();
      }
    }
    c.close();
  }
  
  public PaymentMethod() {
    this.paymentType = NEUTRAL;
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
    if (predef != null) {
      throw new UnsupportedOperationException();
    }
    this.label = label;
  }
  public String getDisplayLabel(Context ctx) {
    if (predef == null)
      return label;
    switch (predef) {
    case CHEQUE: return ctx.getString(R.string.pm_cheque);
    case CREDITCARD: return ctx.getString(R.string.pm_creditcard);
    case DEPOSIT: return ctx.getString(R.string.pm_deposit);
    case DIRECTDEBIT: return ctx.getString(R.string.pm_directdebit);
    }
    return label;
  }
  static HashMap<Long,PaymentMethod> methods = new HashMap<Long,PaymentMethod>();
  
  public static PaymentMethod getInstanceFromDb(long id) throws DataObjectNotFoundException {
    PaymentMethod method;
    method = methods.get(id);
    if (method != null) {
      return method;
    }
    method = new PaymentMethod(id);
    methods.put(id, method);
    return method;
  }
  
  public Uri save() {
    Uri uri;
    ContentValues initialValues = new ContentValues();
    initialValues.put("label", label);
    initialValues.put("type",paymentType);
    if (id == 0) {
      uri = MyApplication.cr().insert(CONTENT_URI, initialValues);
      id = Integer.valueOf(uri.getLastPathSegment());
    } else {
      uri = CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
      MyApplication.cr().update(uri,initialValues,null,null);
    }
    setMethodAccountTypes();
    if (!methods.containsKey(id))
      methods.put(id, this);
    return uri;
  }
  private void setMethodAccountTypes() {
    MyApplication.cr().delete(TransactionProvider.ACCOUNTTYPES_METHODS_URI, "method_id = ?", new String[]{String.valueOf(id)});
    ContentValues initialValues = new ContentValues();
    initialValues.put("method_id", id);
    for (Account.Type accountType : accountTypes) {
      initialValues.put("type",accountType.name());
      MyApplication.cr().insert(TransactionProvider.ACCOUNTTYPES_METHODS_URI, initialValues);
    }
  }

  /**
   * empty the cache
   */
  public static void clear() {
    methods.clear();
  }
  public static boolean delete(long id) {
    MyApplication.cr().delete(TransactionProvider.ACCOUNTTYPES_METHODS_URI,"method_id = ?",new String[] {String.valueOf(id)});
    return MyApplication.cr().delete(CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build(),
        null, null) > 0;
  }
  public static int count(String selection,String[] selectionArgs) {
    Cursor mCursor = MyApplication.cr().query(TransactionProvider.ACCOUNTTYPES_METHODS_URI,new String[] {"count(*)"},
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
}
