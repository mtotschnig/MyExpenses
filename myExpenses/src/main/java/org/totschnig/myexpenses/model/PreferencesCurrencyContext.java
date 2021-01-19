package org.totschnig.myexpenses.model;

import android.os.Build;

import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.locale.UserLocaleProvider;

import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

public class PreferencesCurrencyContext implements CurrencyContext {
  /**
   * used with currencies where Currency.getDefaultFractionDigits returns -1
   */
  public static final int DEFAULTFRACTIONDIGITS = 8;
  private static final String KEY_CUSTOM_FRACTION_DIGITS = "CustomFractionDigits";
  private static final String KEY_CUSTOM_CURRENCY_SYMBOL = "CustomCurrencySymbol";
  final private PrefHandler prefHandler;
  final private UserLocaleProvider userLocaleProvider;
  private static final Map<String, CurrencyUnit> INSTANCES = Collections.synchronizedMap(new HashMap<>());

  public PreferencesCurrencyContext(PrefHandler prefHandler, UserLocaleProvider userLocaleProvider) {
    this.prefHandler = prefHandler;
    this.userLocaleProvider = userLocaleProvider;
  }

  @Override
  @NonNull
  public CurrencyUnit get(@NonNull String currencyCode) {
    synchronized (this) {
      CurrencyUnit currencyUnit = INSTANCES.get(currencyCode);
      if (currencyUnit != null) {
        return currencyUnit;
      }

      Currency c = Utils.getInstance(currencyCode);
      if (c != null) {
        currencyUnit = new CurrencyUnit(currencyCode, getSymbol(c), getFractionDigits(c), getDisplayName(c));
      } else {
        final String customSymbol = getCustomSymbol(currencyCode);
        final int customFractionDigits = getCustomFractionDigits(currencyCode);
        currencyUnit = new CurrencyUnit(currencyCode, customSymbol == null ? "¤" : customSymbol,
            customFractionDigits == -1 ? DEFAULTFRACTIONDIGITS : customFractionDigits);
      }
      INSTANCES.put(currencyCode, currencyUnit);
      return currencyUnit;
    }
  }

  private String getDisplayName(Currency c) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      return c.getDisplayName();
    } else {
      return c.getCurrencyCode();
    }
  }

  public String getCustomSymbol(String currencyCode) {
    return prefHandler.getString(currencyCode + KEY_CUSTOM_CURRENCY_SYMBOL, null);
  }

  public int getCustomFractionDigits(String currencyCode) {
    return prefHandler.getInt(currencyCode + KEY_CUSTOM_FRACTION_DIGITS, -1);
  }

  public String getSymbol(@NonNull Currency currency) {
    String custom = getCustomSymbol(currency.getCurrencyCode());
    return custom != null ? custom : currency.getSymbol(userLocaleProvider.getUserPreferredLocale());
  }

  public int getFractionDigits(Currency currency) {
    int customFractionDigits = getCustomFractionDigits(currency.getCurrencyCode());
    if (customFractionDigits != -1) {
      return customFractionDigits;
    }
    int digits = currency.getDefaultFractionDigits();
    if (digits != -1) {
      return digits;
    }
    return DEFAULTFRACTIONDIGITS;
  }

  @Override
  public void storeCustomFractionDigits(String currencyCode, int fractionDigits) {
    prefHandler.putInt(currencyCode + KEY_CUSTOM_FRACTION_DIGITS, fractionDigits);
    INSTANCES.remove(currencyCode);
  }

  @Override
  public void storeCustomSymbol(String currencyCode, String symbol) {
    Currency currency = null;
    try {
      currency = Currency.getInstance(currencyCode);
    } catch (Exception ignored) {
    }
    String key = currencyCode + KEY_CUSTOM_CURRENCY_SYMBOL;
    if (currency != null && currency.getSymbol().equals(symbol)) {
      prefHandler.remove(key);
    } else {
      prefHandler.putString(key, symbol);
    }
    INSTANCES.remove(currencyCode);
  }

  @Override
  public void ensureFractionDigitsAreCached(CurrencyUnit currency) {
    storeCustomFractionDigits(currency.getCode(), currency.getFractionDigits());
  }

  @Override
  public void invalidateHomeCurrency() {
    INSTANCES.remove(AggregateAccount.AGGREGATE_HOME_CURRENCY_CODE);
  }
}
