package org.totschnig.myexpenses.util;

import android.content.ContentResolver;
import android.text.TextUtils;

import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.locale.UserLocaleProvider;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CurrencyFormatter {
  private PrefHandler prefHandler;
  private UserLocaleProvider userLocaleProvider;

  @Inject
  public CurrencyFormatter(PrefHandler prefHandler, UserLocaleProvider userLocaleProvider) {
    this.prefHandler = prefHandler;
    this.userLocaleProvider = userLocaleProvider;
  }

  private Map<String, NumberFormat> numberFormats = new HashMap<>();

  public void invalidate(String currency, ContentResolver contentResolver) {
    numberFormats.remove(currency);
    notifyUris(contentResolver);
  }

  public void invalidateAll(ContentResolver contentResolver) {
    numberFormats.clear();
    notifyUris(contentResolver);
  }

  private void notifyUris(ContentResolver contentResolver) {
    contentResolver.notifyChange(TransactionProvider.TEMPLATES_URI, null, false);
    contentResolver.notifyChange(TransactionProvider.TRANSACTIONS_URI, null, false);
    contentResolver.notifyChange(TransactionProvider.ACCOUNTS_URI, null, false);
    contentResolver.notifyChange(TransactionProvider.UNCOMMITTED_URI, null, false);
  }

  private NumberFormat initNumberFormat() {
    String prefFormat = prefHandler.getString(PrefKey.CUSTOM_DECIMAL_FORMAT, "");
    if (!prefFormat.equals("")) {
      DecimalFormat nf = new DecimalFormat();
      try {
        nf.applyLocalizedPattern(prefFormat);
        return nf;
      } catch (IllegalArgumentException ignored) {
        //fallback to default currency instance
      }
    }
    return NumberFormat.getCurrencyInstance(userLocaleProvider.getUserPreferredLocale());
  }

  private NumberFormat getNumberFormat(CurrencyUnit currencyUnit) {
    NumberFormat numberFormat = numberFormats.get(currencyUnit.getCode());
    if (numberFormat == null) {
      numberFormat = initNumberFormat();
      int fractionDigits = currencyUnit.getFractionDigits();
      try {
        numberFormat.setCurrency(Currency.getInstance(currencyUnit.getCode()));
      } catch (Exception ignored) { /*Custom locale}*/ }
      if (fractionDigits <= 3) {
        numberFormat.setMinimumFractionDigits(fractionDigits);
        numberFormat.setMaximumFractionDigits(fractionDigits);
      } else {
        numberFormat.setMaximumFractionDigits(fractionDigits);
      }
      String currencySymbol = currencyUnit.getSymbol();
      if (currencySymbol != null) {
        DecimalFormatSymbols decimalFormatSymbols = ((DecimalFormat) numberFormat).getDecimalFormatSymbols();
        decimalFormatSymbols.setCurrencySymbol(currencySymbol);
        ((DecimalFormat) numberFormat).setDecimalFormatSymbols(decimalFormatSymbols);
      }
      numberFormats.put(currencyUnit.getCode(), numberFormat);
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
    return formatCurrency(amount, money.getCurrencyUnit());
  }

  public String formatCurrency(BigDecimal amount, CurrencyUnit currency) {
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
  @Deprecated
  public String convAmount(String text, CurrencyUnit currency) {
    try {
      return convAmount(TextUtils.isEmpty(text) ? 0 : Double.valueOf(text).longValue(), currency);
    } catch (NumberFormatException e) {
      CrashHandler.report(e);
      return text;
    }
  }

  /**
   * utility method that calls formatters for amount this method can be called
   * directly with Long values retrieved from db
   *
   * @param amount
   * @param currency
   * @return formated string
   */
  public String convAmount(Long amount, CurrencyUnit currency) {
    return formatCurrency(new Money(currency, amount));
  }
}
