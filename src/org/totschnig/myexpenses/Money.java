package org.totschnig.myexpenses;

import java.math.BigDecimal;
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
  public void setAmountMajor(BigDecimal amountMajor) {
    this.amountMinor = amountMajor.multiply(new BigDecimal(100)).longValue();
  }
  public BigDecimal getAmountMajor() {
    BigDecimal bd = new BigDecimal(amountMinor);
    bd.setScale(2);
    return bd.divide(new BigDecimal(100));
  }
}
