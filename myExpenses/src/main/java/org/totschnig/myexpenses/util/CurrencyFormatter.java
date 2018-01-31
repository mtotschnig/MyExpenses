package org.totschnig.myexpenses.util;

import android.content.ContentResolver;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.TransactionProvider;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

public class CurrencyFormatter {

  private static CurrencyFormatter INSTANCE = new CurrencyFormatter();

  public static CurrencyFormatter instance() {
    return INSTANCE;
  }

  private CurrencyFormatter() {
  }

  private Map<String, NumberFormat> numberFormats = new HashMap<>();

  public void invalidate(String currency) {
    numberFormats.remove(currency);
    notifyUris();
  }

  public void invalidateAll() {
    numberFormats.clear();
    notifyUris();
  }

  private void notifyUris() {
    ContentResolver contentResolver = MyApplication.getInstance().getContentResolver();
    contentResolver.notifyChange(TransactionProvider.TEMPLATES_URI, null, false);
    contentResolver.notifyChange(TransactionProvider.TRANSACTIONS_URI, null, false);
    contentResolver.notifyChange(TransactionProvider.ACCOUNTS_URI, null, false);
    contentResolver.notifyChange(TransactionProvider.UNCOMMITTED_URI, null, false);
  }

  private NumberFormat initNumberFormat() {
    String prefFormat = PrefKey.CUSTOM_DECIMAL_FORMAT.getString("");
    if (!prefFormat.equals("")) {
      DecimalFormat nf = new DecimalFormat();
      try {
        nf.applyLocalizedPattern(prefFormat);
        return nf;
      } catch (IllegalArgumentException ignored) {
        //fallback to default currency instance
      }
    }
    return NumberFormat.getCurrencyInstance();
  }

  private NumberFormat getNumberFormat(Currency currency) {
    NumberFormat numberFormat = numberFormats.get(currency.getCurrencyCode());
    if (numberFormat == null) {
      numberFormat = initNumberFormat();
      int fractionDigits = Money.getFractionDigits(currency);
      numberFormat.setCurrency(currency);
      if (fractionDigits <= 3) {
        numberFormat.setMinimumFractionDigits(fractionDigits);
        numberFormat.setMaximumFractionDigits(fractionDigits);
      } else {
        numberFormat.setMaximumFractionDigits(fractionDigits);
      }
      String currencySymbol = Money.getCustomSymbol(currency.getCurrencyCode());
      if (currencySymbol != null) {
        DecimalFormatSymbols decimalFormatSymbols = ((DecimalFormat) numberFormat).getDecimalFormatSymbols();
        decimalFormatSymbols.setCurrencySymbol(currencySymbol);
        ((DecimalFormat) numberFormat).setDecimalFormatSymbols(decimalFormatSymbols);
      }
      numberFormats.put(currency.getCurrencyCode(), numberFormat);
    }
    return numberFormat;
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

  public String formatCurrency(BigDecimal amount, Currency currency) {
    return getNumberFormat(currency).format(amount);
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
