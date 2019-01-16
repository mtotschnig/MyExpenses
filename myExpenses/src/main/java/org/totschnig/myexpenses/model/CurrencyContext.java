package org.totschnig.myexpenses.model;

public interface CurrencyContext {
  CurrencyUnit get(String currencyCode);

  void storeCustomFractionDigits(String currencyCode, int fractionDigits);

  void storeCustomSymbol(String currencyCode, String symbol);

  void ensureFractionDigitsAreCached(CurrencyUnit currency);

  void invalidateHomeCurrency();
}
