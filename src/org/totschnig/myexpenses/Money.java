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
    int scale = currency.getDefaultFractionDigits();
    this.amountMinor = amountMajor.multiply(new BigDecimal(Math.pow(10,scale))).longValue();
  }
  public BigDecimal getAmountMajor() {
    BigDecimal bd = new BigDecimal(amountMinor);
    int scale = currency.getDefaultFractionDigits();
    if (scale != -1) {
      bd.setScale(scale);
      return bd.divide(new BigDecimal(Math.pow(10,scale)));
    }
    return bd;
  }
}
