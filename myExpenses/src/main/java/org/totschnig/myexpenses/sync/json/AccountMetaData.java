package org.totschnig.myexpenses.sync.json;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;

@AutoValue
public abstract class AccountMetaData implements Parcelable {
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

  @Override
  public String toString() {
    return label() + " (" + currency() + ")";
  }

  public Account toAccount() {
    AccountType accountType;
    try {
      accountType = AccountType.valueOf(type());
    } catch (IllegalArgumentException e) {
      accountType = AccountType.CASH;
    }
    Account account = new Account(label(),
        org.totschnig.myexpenses.util.Utils.getSaveInstance(currency()),
        openingBalance(), description(), accountType, color());
    account.uuid = uuid();
    return account;
  }

  public static AccountMetaData from(Account account) {
    return builder().setCurrency(account.currency.getCurrencyCode()).setColor(account.color)
        .setUuid(account.uuid).setDescription(account.description).setLabel(account.label)
        .setOpeningBalance(account.openingBalance.getAmountMinor()).setType(account.type.name())
        .build();
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

    public abstract AccountMetaData build();
  }
}
