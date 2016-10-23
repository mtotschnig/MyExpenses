package org.totschnig.myexpenses.sync.json;


import android.database.Cursor;
import android.support.annotation.Nullable;

import com.annimon.stream.Stream;
import com.gabrielittner.auto.value.cursor.ColumnAdapter;
import com.gabrielittner.auto.value.cursor.ColumnName;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import org.totschnig.myexpenses.util.TextUtils;

import java.util.ArrayList;
import java.util.List;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHOD_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENT_UUID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PICTURE_URI;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TIMESTAMP;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID;

@AutoValue
public abstract class TransactionChange {

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

  @ColumnName(KEY_AMOUNT)
  @Nullable
  public abstract Long amount();

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
  public abstract List<TransactionChange> splitParts();

  public static TransactionChange mergeUpdate(TransactionChange initial, TransactionChange change) {
    if (!(change.isUpdate() && initial.isUpdate())) {
      throw new IllegalStateException("Can only merge updates");
    }
    if (!initial.uuid().equals(change.uuid())) {
      throw new IllegalStateException("Can only merge changes with same uuid");
    }
    Builder builder = initial.toBuilder();
    if (change.parentUuid() != null) {
      builder.setParentUuid(change.parentUuid());
    }
    if (change.comment() != null) {
      builder.setComment(change.comment());
    }
    if (change.date() != null) {
      builder.setDate(change.date());
    }
    if (change.amount() != null) {
      builder.setAmount(change.amount());
    }
    if (change.label() != null) {
      builder.setLabel(change.label());
    }
    if (change.payeeName() != null) {
      builder.setPayeeName(change.payeeName());
    }
    if (change.transferAccount() != null) {
      builder.setTransferAccount(change.transferAccount());
    }
    if (change.methodLabel() != null) {
      builder.setMethodLabel(change.methodLabel());
    }
    if (change.crStatus() != null) {
      builder.setCrStatus(change.crStatus());
    }
    if (change.referenceNumber() != null) {
      builder.setReferenceNumber(change.referenceNumber());
    }
    if (change.pictureUri() != null) {
      builder.setPictureUri(change.pictureUri());
    }
    return builder.setTimeStamp(System.currentTimeMillis()).build();
  }

  public enum Type {
    created, updated, deleted;

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

  public boolean isDelete() {
    return type().equals(Type.deleted);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setType(Type value);
    public abstract Builder setUuid(String value);
    abstract String uuid();
    public abstract Builder setTimeStamp(Long value);
    public abstract Builder setParentUuid(String value);
    public abstract Builder setComment(String value);
    public abstract Builder setAmount(Long value);
    public abstract Builder setDate(Long value);
    public abstract Builder setLabel(String value);
    public abstract Builder setPayeeName(String value);
    public abstract Builder setTransferAccount(String value);
    public abstract Builder setMethodLabel(String value);
    public abstract Builder setCrStatus(String value);
    public abstract Builder setReferenceNumber(String value);
    public abstract Builder setPictureUri(String value);
    public abstract Builder setSplitParts(List<TransactionChange> value);
    public Builder setSplitPartsAndValidate(List<TransactionChange> value) {
      if (Stream.of(value).allMatch(value1 -> value1.parentUuid().equals(uuid()))) {
        return setSplitParts(value);
      } else {
        throw new IllegalStateException("parts parentUuid does not mactch parents uuid");
      }
    }
    public abstract TransactionChange build();
  }
}
