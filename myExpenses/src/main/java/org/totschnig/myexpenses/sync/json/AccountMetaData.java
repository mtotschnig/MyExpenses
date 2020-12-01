package org.totschnig.myexpenses.sync.json;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.preference.PrefKey;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@AutoValue
public abstract class  AccountMetaData implements Parcelable {
  public static TypeAdapter<AccountMetaData> typeAdapter(Gson gson) {
    return new AutoValue_AccountMetaData.GsonTypeAdapter(gson);
  }

  public static Builder builder() {
    return new AutoValue_AccountMetaData.Builder();
  }

  public abstract String label();

  public abstract String currency();

  public abstract int color();

  public abstract String uuid();

  public abstract long openingBalance();

  public abstract String description();

  public abstract String type();

  @Nullable
  public abstract Double exchangeRate();

  @Nullable
  public abstract String exchangeRateOtherCurrency();

  @Nullable
  abstract Boolean excludeFromTotals();

  @Nullable
  abstract Long criterion();

  public long _criterion() {
    return criterion() == null ? 0L : criterion();
  }

  public boolean _excludeFromTotals() {
    return excludeFromTotals() != null && excludeFromTotals();
  }

  @NonNull
  @Override
  public String toString() {
    return label() + " (" + currency() + ")";
  }

  public Account toAccount(CurrencyContext currencyContext) {
    AccountType accountType;
    try {
      accountType = AccountType.valueOf(type());
    } catch (IllegalArgumentException e) {
      accountType = AccountType.CASH;
    }
    final CurrencyUnit currency = currencyContext.get(currency());
    Account account = new Account(label(), currency, openingBalance(), description(), accountType, color());
    account.setUuid(uuid());
    if (_criterion() != 0) {
      account.setCriterion(new Money(currency, _criterion()));
    }
    account.excludeFromTotals = _excludeFromTotals();
    String homeCurrency = PrefKey.HOME_CURRENCY.getString(null);
    final Double exchangeRate = exchangeRate();
    if (exchangeRate != null && homeCurrency != null && homeCurrency.equals(exchangeRateOtherCurrency())) {
      account.setExchangeRate(exchangeRate);
    }
    return account;
  }

  public static AccountMetaData from(Account account) {
    String homeCurrency = PrefKey.HOME_CURRENCY.getString(null);
    final String accountCurrency = account.getCurrencyUnit().getCode();
    final Builder builder = builder()
        .setCurrency(accountCurrency)
        .setColor(account.color)
        .setUuid(account.getUuid())
        .setDescription(account.description)
        .setLabel(account.getLabel())
        .setOpeningBalance(account.openingBalance.getAmountMinor())
        .setType(account.getType().name())
        .setExcludeFromTotals(account.excludeFromTotals)
        .setCriterion(account.getCriterion() != null ? account.getCriterion().getAmountMinor() : 0);
    if (homeCurrency != null && !homeCurrency.equals(accountCurrency)) {
      builder.setExchangeRate(account.getExchangeRate()).setExchangeRateOtherCurrency(homeCurrency);
    }
    return builder.build();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setLabel(String label);
    public abstract Builder setCurrency(String currency);
    public abstract Builder setColor(int color);
    public abstract Builder setUuid(String uuid);
    public abstract Builder setOpeningBalance(long openingBalance);
    public abstract Builder setDescription(String description);
    public abstract Builder setType(String type);
    public abstract Builder setExchangeRate(Double exchangeRate);
    public abstract Builder setExchangeRateOtherCurrency(String otherCurrency);
    public abstract Builder setExcludeFromTotals(Boolean excludeFromTotals);
    public abstract Builder setCriterion(Long criterion);

    public abstract AccountMetaData build();
  }
}
