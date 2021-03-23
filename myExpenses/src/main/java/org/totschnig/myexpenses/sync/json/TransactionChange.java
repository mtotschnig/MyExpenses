package org.totschnig.myexpenses.sync.json;


import android.database.Cursor;

import com.annimon.stream.Stream;
import com.gabrielittner.auto.value.cursor.ColumnAdapter;
import com.gabrielittner.auto.value.cursor.ColumnName;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.util.TextUtils;

import java.util.List;

import androidx.annotation.Nullable;

import static org.totschnig.myexpenses.provider.DatabaseConstants.FULL_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHOD_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENT_UUID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PICTURE_URI;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TIMESTAMP;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TRANSFER_ACCOUNT_UUUID;

@AutoValue
public abstract class TransactionChange {

  public static final String[] PROJECTION = new String[]{
      KEY_TYPE,
      KEY_UUID,
      KEY_TIMESTAMP,
      KEY_PARENT_UUID,
      "NULLIF(TRIM(" + KEY_COMMENT + "),'') AS " + KEY_COMMENT,
      KEY_DATE,
      KEY_VALUE_DATE,
      KEY_AMOUNT,
      KEY_ORIGINAL_AMOUNT,
      KEY_ORIGINAL_CURRENCY,
      KEY_EQUIVALENT_AMOUNT,
      FULL_LABEL,
      "NULLIF(TRIM(" + KEY_PAYEE_NAME + "),'') AS " + KEY_PAYEE_NAME,
      TRANSFER_ACCOUNT_UUUID,
      KEY_METHOD_LABEL,
      KEY_CR_STATUS,
      "NULLIF(TRIM(" + KEY_REFERENCE_NUMBER + "),'') AS " + KEY_REFERENCE_NUMBER,
      KEY_PICTURE_URI
  };

  public static TransactionChange create(Cursor cursor) {
    final AutoValue_TransactionChange fromCursor = AutoValue_TransactionChange.createFromCursor(cursor);
    if (fromCursor.equivalentAmount() == null) {
      return fromCursor;
    }
    final String homeCurrency = PrefKey.HOME_CURRENCY.getString(null);
    final Builder builder = fromCursor.toBuilder();
    if (homeCurrency != null) {
      builder.setEquivalentCurrency(homeCurrency);
    } else {
      builder.setEquivalentAmount(null);
    }
    return builder.setEquivalentCurrency(homeCurrency).build();
  }

  public static TypeAdapter<TransactionChange> typeAdapter(Gson gson) {
    return new AutoValue_TransactionChange.GsonTypeAdapter(gson);
  }

  public static Builder builder() {
    return new AutoValue_TransactionChange.Builder();
  }

  public abstract Builder toBuilder();

  @Nullable
  public abstract String appInstance();

  @ColumnAdapter(ChangeTypeAdapter.class)
  public abstract Type type();

  @ColumnName(KEY_UUID)
  public abstract String uuid();

  @ColumnName(KEY_TIMESTAMP)
  public abstract Long timeStamp();

  @ColumnName(KEY_PARENT_UUID)
  @Nullable
  public abstract String parentUuid();

  @ColumnName(KEY_COMMENT)
  @Nullable
  public abstract String comment();

  @ColumnName(KEY_DATE)
  @Nullable
  public abstract Long date();

  @ColumnName(KEY_VALUE_DATE)
  @Nullable
  public abstract Long valueDate();

  @ColumnName(KEY_AMOUNT)
  @Nullable
  public abstract Long amount();

  @ColumnName(KEY_ORIGINAL_AMOUNT)
  @Nullable
  public abstract Long originalAmount();

  @ColumnName(KEY_ORIGINAL_CURRENCY)
  @Nullable
  public abstract String originalCurrency();

  @ColumnName(KEY_EQUIVALENT_AMOUNT)
  @Nullable
  public abstract Long equivalentAmount();

  @Nullable
  public abstract String equivalentCurrency();

  @ColumnName(KEY_LABEL)
  @Nullable
  public abstract String label();

  @ColumnName(KEY_PAYEE_NAME)
  @Nullable
  public abstract String payeeName();

  @ColumnName(KEY_TRANSFER_ACCOUNT)
  @Nullable
  public abstract String transferAccount();

  @ColumnName(KEY_METHOD_LABEL)
  @Nullable
  public abstract String methodLabel();

  @ColumnName(KEY_CR_STATUS)
  @Nullable
  public abstract String crStatus();

  @ColumnName(KEY_REFERENCE_NUMBER)
  @Nullable
  public abstract String referenceNumber();

  @ColumnName(KEY_PICTURE_URI)
  @Nullable
  public abstract String pictureUri();

  @Nullable
  public abstract List<String> tags();

  @Nullable
  public abstract List<TransactionChange> splitParts();

  public boolean isEmpty() {
    final Long equivalentAmount = equivalentAmount();
    return isCreateOrUpdate() && comment() == null && date() == null && amount() == null &&
        label() == null && payeeName() == null && transferAccount() == null && methodLabel() == null &&
        crStatus() == null && referenceNumber() == null && pictureUri() == null && splitParts() == null
        && originalAmount() == null && (equivalentAmount == null || equivalentAmount == 0L)
        && parentUuid() == null;
        //we ignore changes of equivalent amount which result from change of home currency
  }

  public enum Type {
    created, updated, deleted, unsplit, metadata, link;

    public static final String JOIN;

    static {
      JOIN = TextUtils.joinEnum(Type.class);
    }
  }

  public boolean isCreate() {
    return type().equals(Type.created);
  }

  public boolean isUpdate() {
    return type().equals(Type.updated);
  }

  public boolean isCreateOrUpdate() {
    return isCreate() || isUpdate();
  }

  public boolean isDelete() {
    return type().equals(Type.deleted);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setAppInstance(String value);

    public abstract Builder setType(Type value);

    public abstract Builder setUuid(String value);

    abstract String uuid();

    public abstract Builder setTimeStamp(Long value);

    public abstract Builder setParentUuid(String value);

    public abstract Builder setComment(String value);

    public abstract Builder setAmount(Long value);

    public abstract Builder setOriginalAmount(Long value);

    public abstract Builder setOriginalCurrency(String value);

    public abstract Builder setEquivalentAmount(Long value);

    public abstract Builder setEquivalentCurrency(String value);

    public abstract Builder setDate(Long value);

    public abstract Builder setValueDate(Long value);

    public abstract Builder setLabel(String value);

    public abstract Builder setPayeeName(String value);

    public abstract Builder setTransferAccount(String value);

    public abstract Builder setMethodLabel(String value);

    public abstract Builder setCrStatus(String value);

    public abstract Builder setReferenceNumber(String value);

    public abstract Builder setPictureUri(String value);

    public abstract Builder setSplitParts(List<TransactionChange> value);

    public abstract Builder setTags(List<String> vale);

    public Builder setSplitPartsAndValidate(List<TransactionChange> value) {
      if (Stream.of(value).allMatch(value1 -> value1.parentUuid().equals(uuid()))) {
        return setSplitParts(value);
      } else {
        throw new IllegalStateException("parts parentUuid does not match parents uuid");
      }
    }

    public Builder setCurrentTimeStamp() {
      return setTimeStamp(System.currentTimeMillis() / 1000);
    }

    public abstract TransactionChange build();
  }
}
