package org.totschnig.myexpenses.util;

import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.preference.PrefKey;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Currency;

import hugo.weaving.DebugLog;

public class CurrencyFormatter {

  private  NumberFormat numberFormat;

  private void initNumberFormat() {
    String prefFormat = PrefKey.CUSTOM_DECIMAL_FORMAT.getString("");
    if (!prefFormat.equals("")) {
      DecimalFormat nf = new DecimalFormat();
      try {
        nf.applyLocalizedPattern(prefFormat);
        numberFormat = nf;
      } catch (IllegalArgumentException e) {
        //fallback to default currency instance
        numberFormat = NumberFormat.getCurrencyInstance();
      }
    } else {
      numberFormat = NumberFormat.getCurrencyInstance();
    }
  }

  private  NumberFormat getNumberFormat() {
    if (numberFormat == null) {
      initNumberFormat();
    }
    return numberFormat;
  }

  public  void setNumberFormat(NumberFormat in) {
    numberFormat = in;
  }


  /**
   * formats an amount with a currency
   *
   * @param money
   * @return formated string
   */
  public String formatCurrency(Money money) {
    BigDecimal amount = money.getAmountMajor();
    Currency currency = money.getCurrency();
    return formatCurrency(amount, currency);
  }

  @DebugLog
  public String formatCurrency(BigDecimal amount, Currency currency) {
    NumberFormat nf = getNumberFormat();
    int fractionDigits = Money.getFractionDigits(currency);
    nf.setCurrency(currency);
    DecimalFormatSymbols decimalFormatSymbols = ((DecimalFormat) nf).getDecimalFormatSymbols();
    decimalFormatSymbols.setCurrencySymbol("₨");
    ((DecimalFormat) nf).setDecimalFormatSymbols(decimalFormatSymbols);
    if (fractionDigits <= 3) {
      nf.setMinimumFractionDigits(fractionDigits);
      nf.setMaximumFractionDigits(fractionDigits);
    } else {
      nf.setMaximumFractionDigits(fractionDigits);
    }
    return nf.format(amount);
  }

  /**
   * utility method that calls formatters for amount this method is called from
   * adapters that give us the amount as String
   *
   * @param text     amount as String
   * @param currency
   * @return formated string
   */
  public String convAmount(String text, Currency currency) {
    Long amount;
    try {
      amount = Long.valueOf(text);
    } catch (NumberFormatException e) {
      amount = 0L;
    }
    return convAmount(amount, currency);
  }

  /**
   * utility method that calls formatters for amount this method can be called
   * directly with Long values retrieved from db
   *
   * @param amount
   * @param currency
   * @return formated string
   */
  public String convAmount(Long amount, Currency currency) {
    return formatCurrency(new Money(currency, amount));
  }
}
