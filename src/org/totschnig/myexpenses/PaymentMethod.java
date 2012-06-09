package org.totschnig.myexpenses;

import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.util.Log;

public class PaymentMethod {
  public long id;
  private String label;
  final int EXPENSE =  -1;
  final int NEUTRAL = 0;
  final int INCOME = 1;
  private int type;
  public PreDefined predef;
  private static ExpensesDbAdapter mDbHelper  = MyApplication.db();
  
  public enum PreDefined {
    CHEQUE(-1),CREDITCARD(-1),DEPOSIT(1),DIRECTDEBIT(-1);
    public final int type;
    PreDefined(int type) {
      this.type = type;
    }
  }
  private PaymentMethod(long id) throws DataObjectNotFoundException {
    this.id = id;
    Cursor c = mDbHelper.fetchPaymentMethod(id);
    if (c.getCount() == 0) {
      throw new DataObjectNotFoundException();
    }
    
    this.label = c.getString(c.getColumnIndexOrThrow("label"));
    this.type = c.getInt(c.getColumnIndexOrThrow("type"));
    c.close();
    try {
      predef = PreDefined.valueOf(this.label);
    } catch (IllegalArgumentException ex) { 
      predef = null;
    }
  }
  
  public PaymentMethod() {
    this.type = NEUTRAL;
  }
  public int getType() {
    return type;
  }
  public void setType(int type) {
    this.type = type;
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
          type
          );
    } else {
      mDbHelper.updateMethod(
          id,
          label,
          type
          );
    }
    if (!methods.containsKey(id))
      methods.put(id, this);
    return id;
  }
}
