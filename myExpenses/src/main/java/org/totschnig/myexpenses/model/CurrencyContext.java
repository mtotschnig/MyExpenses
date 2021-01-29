package org.totschnig.myexpenses.model;

import androidx.annotation.NonNull;

public interface CurrencyContext {
  @NonNull CurrencyUnit get(@NonNull String currencyCode);

  void storeCustomFractionDigits(String currencyCode, int fractionDigits);

  void storeCustomSymbol(String currencyCode, String symbol);

  void ensureFractionDigitsAreCached(CurrencyUnit currency);

  void invalidateHomeCurrency();
}
