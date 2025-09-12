package org.totschnig.myexpenses.sync.json;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.model.SortDirection;
import org.totschnig.myexpenses.model2.Account;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;

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

  public Account toAccount(String homeCurrency, String syncAccount) {
    Double exchangeRate = exchangeRate();
    if (exchangeRate == null || !homeCurrency.equals(exchangeRateOtherCurrency())) {
      exchangeRate = 1.0;
    }
    return new Account(
            0L,
            label(),
            description(),
            openingBalance(),
            currency(),
            AccountType.Companion.withName(type()),
            color(),
            _criterion(),
            syncAccount,
            false,
            uuid(),
            false,
            KEY_DATE,
            SortDirection.DESC,
            exchangeRate,
            Grouping.NONE,
            null,
            false
    );
  }

  public static AccountMetaData from(Account account, String homeCurrency) {
    final String accountCurrency = account.getCurrency();
    final Builder builder = builder()
        .setCurrency(accountCurrency)
        .setColor(account.getColor())
        .setUuid(account.getUuid())
        .setDescription(account.getDescription())
        .setLabel(account.getLabel())
        .setOpeningBalance(account.getOpeningBalance())
        .setType(account.getType().getName())
        .setExcludeFromTotals(account.getExcludeFromTotals())
        .setCriterion(account.getCriterion() != null ? account.getCriterion() : 0);
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
