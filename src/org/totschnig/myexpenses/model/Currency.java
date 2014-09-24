package org.totschnig.myexpenses.model;

import android.net.Uri;

public class Currency extends Model {
  private String code;
  private int fractionDigits;
  private java.util.Currency wrappedCurrency = null;

  public java.util.Currency getWrappedCurrency() {
    return wrappedCurrency;
  }

  public Currency(String code, Integer fractionDigits) {
    super();
    this.code = code;
    try {
      this.wrappedCurrency = java.util.Currency.getInstance(code);
      if (fractionDigits == null) {
        this.fractionDigits = wrappedCurrency.getDefaultFractionDigits();
      } else {
        this.fractionDigits = fractionDigits;
      }
    } catch (IllegalArgumentException e) {
      this.fractionDigits = 0;
    }
  }

  public Currency(String code) {
    this(code,null);
  }

  public String getCode() {
    return code;
  }

  public int getFractionDigits() {
    return fractionDigits;
  }

  @Override
  public Uri save() {
    // TODO Auto-generated method stub
    return null;
  }

}
