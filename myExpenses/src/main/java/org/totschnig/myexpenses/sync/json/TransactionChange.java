package org.totschnig.myexpenses.sync.json;


import android.database.Cursor;
import android.support.annotation.Nullable;

import com.gabrielittner.auto.value.cursor.ColumnName;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import org.totschnig.myexpenses.util.TextUtils;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENT_UUID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PICTURE_URI;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TIMESTAMP;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;
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

  @ColumnName(KEY_TYPE)
  public abstract String type();

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

  @ColumnName(KEY_CATID)
  @Nullable
  public abstract Long catId();

  @ColumnName(KEY_PAYEEID)
  @Nullable
  public abstract Long payeeId();

  @ColumnName(KEY_TRANSFER_ACCOUNT)
  @Nullable
  public abstract Long transferAccount();

  @ColumnName(KEY_METHODID)
  @Nullable
  public abstract Long methodId();

  @ColumnName(KEY_CR_STATUS)
  @Nullable
  public abstract String crStatus();

  @ColumnName(KEY_REFERENCE_NUMBER)
  @Nullable
  public abstract String referenceNumber();

  @ColumnName(KEY_PICTURE_URI)
  @Nullable
  public abstract String pictureUri();

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
    if (change.catId() != null) {
      builder.setCatId(change.catId());
    }
    if (change.payeeId() != null) {
      builder.setPayeeId(change.payeeId());
    }
    if (change.transferAccount() != null) {
      builder.setTransferAccount(change.transferAccount());
    }
    if (change.methodId() != null) {
      builder.setMethodId(change.methodId());
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
    return type().equals(Type.created.name());
  }

  public boolean isUpdate() {
    return type().equals(Type.updated.name());
  }

  public boolean isDelete() {
    return type().equals(Type.deleted.name());
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public Builder setType(Type type) {
      return setType(type.name());
    }
    public abstract Builder setType(String value);
    public abstract Builder setUuid(String value);
    public abstract Builder setTimeStamp(Long value);
    public abstract Builder setParentUuid(String value);
    public abstract Builder setComment(String value);
    public abstract Builder setAmount(Long value);
    public abstract Builder setDate(Long value);
    public abstract Builder setCatId(Long value);
    public abstract Builder setPayeeId(Long value);
    public abstract Builder setTransferAccount(Long value);
    public abstract Builder setMethodId(Long value);
    public abstract Builder setCrStatus(String value);
    public abstract Builder setReferenceNumber(String value);
    public abstract Builder setPictureUri(String value);
    public abstract TransactionChange build();
  }
}
