package org.totschnig.myexpenses.sync.json;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

@AutoValue
public abstract class AccountMetaData {
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

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setLabel(String label);
    public abstract Builder setCurrency(String currency);
    public abstract Builder setColor(int color);
    public abstract Builder setUuid(String uuid);

    public abstract AccountMetaData build();
  }
}
