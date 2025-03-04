package org.totschnig.myexpenses.sync.json;


import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
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
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TIMESTAMP;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TRANSFER_ACCOUNT_UUID;

import android.database.Cursor;

import androidx.annotation.Nullable;

import com.gabrielittner.auto.value.cursor.ColumnAdapter;
import com.gabrielittner.auto.value.cursor.ColumnName;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import org.totschnig.myexpenses.model2.CategoryInfo;
import org.totschnig.myexpenses.util.TextUtils;

import java.util.List;
import java.util.Set;

@AutoValue
public abstract class TransactionChange {

  public static final String[] PROJECTION = new String[]{
      KEY_TYPE,
      KEY_UUID,
      KEY_TIMESTAMP,
      KEY_PARENT_UUID,
      "TRIM(" + KEY_COMMENT + ") AS " + KEY_COMMENT,
      KEY_DATE,
      KEY_VALUE_DATE,
      KEY_AMOUNT,
      KEY_ORIGINAL_AMOUNT,
      KEY_ORIGINAL_CURRENCY,
      KEY_EQUIVALENT_AMOUNT,
      "NULLIF(TRIM(" + KEY_PAYEE_NAME + "),'') AS " + KEY_PAYEE_NAME,
      TRANSFER_ACCOUNT_UUID,
      KEY_CATID,
      KEY_METHOD_LABEL,
      KEY_CR_STATUS,
      KEY_STATUS,
      "TRIM(" + KEY_REFERENCE_NUMBER + ") AS " + KEY_REFERENCE_NUMBER
  };

  public static TransactionChange create(Cursor cursor) {
    return AutoValue_TransactionChange.createFromCursor(cursor);
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

  @ColumnName(KEY_STATUS)
  @Nullable
  public abstract Integer status();

  @ColumnName(KEY_REFERENCE_NUMBER)
  @Nullable
  public abstract String referenceNumber();

  //legacy from pre 3.6.5
  @Nullable
  public abstract String pictureUri();

  @Nullable
  public abstract Set<String> tags();

  @Nullable
  public abstract Set<TagInfo> tagsV2();

  @Nullable
  public abstract Set<String> attachments();

  @Nullable
  public abstract List<TransactionChange> splitParts();

  @Nullable
  public abstract List<CategoryInfo> categoryInfo();

  public boolean isEmpty() {
    final Long equivalentAmount = equivalentAmount();
    return isCreateOrUpdate() && comment() == null && date() == null && amount() == null &&
        label() == null && payeeName() == null && transferAccount() == null && methodLabel() == null &&
        crStatus() == null && referenceNumber() == null && pictureUri() == null && splitParts() == null
        && originalAmount() == null && (equivalentAmount == null || equivalentAmount == 0L)
        && parentUuid() == null && tags() == null  && tagsV2() == null && categoryInfo() == null && attachments() == null
        && status() == null;
        //we ignore changes of equivalent amount which result from change of home currency
  }

  public enum Type {
    created, updated, deleted, unsplit, metadata, link, tags, attachments, unarchive;

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

    public abstract String uuid();

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

    public abstract Builder setStatus(Integer value);

    public abstract Builder setReferenceNumber(String value);

    public abstract Builder setPictureUri(String value);

    public abstract Builder setSplitParts(List<TransactionChange> value);

    public abstract Builder setTags(Set<String> value);

    public abstract Builder setTagsV2(Set<TagInfo> value);

    public abstract Builder setAttachments(Set<String> value);

    public abstract Builder setCategoryInfo(List<CategoryInfo> value);

    public Builder setCurrentTimeStamp() {
      return setTimeStamp(System.currentTimeMillis() / 1000);
    }

    public abstract TransactionChange build();
  }
}
