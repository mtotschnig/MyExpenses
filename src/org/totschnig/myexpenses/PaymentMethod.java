package org.totschnig.myexpenses;

import android.content.res.Resources;

public class PaymentMethod {
  private String label;
  final int EXPENSE =  -1;
  final int INCOME = 1;
  final int NEUTRAL = 0;
  private int type;
  private PreDefined predef;
  
  public enum PreDefined {
    CHEQUE(-1),CREDITCARD(-1),DEPOSIT(1),DIRECTDEBIT(-1);
    public final int type;
    PreDefined(int type) {
      this.type = type;
    }
  }
  public PaymentMethod(String label, int type) {
    this.label = label;
    this.type = type;
    try {
      predef = PreDefined.valueOf(label);
    } catch (IllegalArgumentException ex) { 
      predef = null;
    }
  }
  public int getType() {
    return type;
  }
  public String getLabel() {
    if (predef == null)
      return label;
    switch (predef) {
    case CHEQUE: return Resources.getSystem().getString(R.string.pm_cheque);
    case CREDITCARD: return Resources.getSystem().getString(R.string.pm_creditcard);
    case DEPOSIT: return Resources.getSystem().getString(R.string.pm_deposit);
    case DIRECTDEBIT: return Resources.getSystem().getString(R.string.pm_directdebit);
    }
    return label;
  }
}
