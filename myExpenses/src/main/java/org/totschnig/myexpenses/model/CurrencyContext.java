package org.totschnig.myexpenses.model;

import java.util.Currency;

public interface CurrencyContext {
  CurrencyUnit get(String currencyCode);

  String getCustomSymbol(String currencyCode);

  int getCustomFractionDigits(String currencyCode);

  //TODO check if needed
  String getSymbol(Currency currency);

  //TODO check if needed
  int getFractionDigits(Currency currency);

  void storeCustomFractionDigits(String currencyCode, int fractionDigits);

  boolean storeCustomSymbol(String currencyCode, String symbol);

  void ensureFractionDigitsAreCached(CurrencyUnit currency);
}
