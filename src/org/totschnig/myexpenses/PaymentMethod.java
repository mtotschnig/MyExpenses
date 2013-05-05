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

package org.totschnig.myexpenses;

import java.util.ArrayList;
import java.util.HashMap;


import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class PaymentMethod {
  public long id;
  private String label;
  final int EXPENSE =  -1;
  final int NEUTRAL = 0;
  final int INCOME = 1;
  private int paymentType;
  /**
   * array of account types for which this payment method is applicable
   */
  private ArrayList<Account.Type> accountTypes = new ArrayList<Account.Type>();
  public PreDefined predef;
  private static ExpensesDbAdapter mDbHelper  = MyApplication.db();
  
  public enum PreDefined {
    CHEQUE(-1),CREDITCARD(-1),DEPOSIT(1),DIRECTDEBIT(-1);
    public final int paymentType;
    PreDefined(int paymentType) {
      this.paymentType = paymentType;
    }
  }
  private PaymentMethod(long id) throws DataObjectNotFoundException {
    this.id = id;
    Cursor c = mDbHelper.fetchPaymentMethod(id);
    if (c.getCount() == 0) {
      throw new DataObjectNotFoundException();
    }
    
    this.label = c.getString(c.getColumnIndexOrThrow("label"));
    this.paymentType = c.getInt(c.getColumnIndexOrThrow("type"));
    c.close();
    try {
      predef = PreDefined.valueOf(this.label);
    } catch (IllegalArgumentException ex) { 
      predef = null;
    }
    c = mDbHelper.fetchAccountTypesForPaymentMethod(id);
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
  
  public long save() {
    if (id == 0) {
      id = mDbHelper.createMethod(
          label,
          paymentType, accountTypes
          );
    } else {
      mDbHelper.updateMethod(
          id,
          label,
          paymentType, accountTypes
          );
    }
    if (!methods.containsKey(id))
      methods.put(id, this);
    return id;
  }
}
