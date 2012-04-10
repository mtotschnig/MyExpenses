package org.totschnig.myexpenses;

import java.util.Currency;

public class Money {
  private Currency currency;
  private Long amountMinor;
  
  public Money(Currency currency, Long amountMinor) {
    this.currency = currency;
    this.amountMinor = amountMinor;
  }
  public Currency getCurrency() {
    return currency;
  }
  public void setCurrency(Currency currency) {
    this.currency = currency;
  }
  public Long getAmountMinor() {
    return amountMinor;
  }
  public void setAmountMinor(Long amountMinor) {
    this.amountMinor = amountMinor;
  }
  public void setAmountMajor(Float amountMajor) {
    this.amountMinor = Float.valueOf(amountMajor*100).longValue(); 
  }
  public Float getAmountMajor() {
    return amountMinor.floatValue() / 100;
  }
}
