package org.totschnig.myexpenses.task;

import android.content.ContentUris;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;

import java.util.Currency;

import static org.totschnig.myexpenses.provider.DatabaseConstants.CAT_AS_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

public class LoadAutoFillDataTask extends ExtraTask<LoadAutoFillDataTask.AutoFillData> {
  public static final String KEY_LOAD_AMOUNT = "loadAmount";
  public static final String KEY_LOAD_CATEGORY = "loadCategory";
  public static final String KEY_LOAD_COMMENT = "loadComment";
  public static final String KEY_LOAD_ACCOUNT = "loadAccount";

  LoadAutoFillDataTask(TaskExecutionFragment taskExecutionFragment, int taskId) {
    super(taskExecutionFragment, taskId);
  }

  @Override
  protected AutoFillData doInBackground(Bundle... bundles) {
    Bundle extra = bundles[0];
    final String[] projection = new String[]{KEY_CURRENCY, KEY_AMOUNT, KEY_CATID, CAT_AS_LABEL,
        KEY_COMMENT, KEY_ACCOUNTID};
    Cursor c = MyApplication.getInstance().getContentResolver().query(
        ContentUris.withAppendedId(TransactionProvider.AUTOFILL_URI, extra.getLong(KEY_ROWID)),
        projection, null, null, null);
    AutoFillData autoFillData = null;
    if (c != null) {
      if (c.moveToFirst()) {
        Money amount = extra.getBoolean(KEY_LOAD_AMOUNT) ?
            new Money(Currency.getInstance(c.getString(0)), c.getLong(1)) : null;
        Long categoryId = null;
        String label = null;
        if (extra.getBoolean(KEY_LOAD_CATEGORY)) {
          categoryId = DbUtils.getLongOrNull(c, 2);
          label = c.getString(3);
        }
        String comment = extra.getBoolean(KEY_LOAD_COMMENT) ?
            c.getString(4) : null;
        Long accountId = extra.getBoolean(KEY_LOAD_ACCOUNT) ? DbUtils.getLongOrNull(c, 5) : null;
        autoFillData = AutoFillData.create(amount, categoryId, label, comment, accountId);
      }
      c.close();
    }
    return autoFillData;
  }

  @AutoValue
  public abstract static class AutoFillData {
    @Nullable public abstract Money amount();
    @Nullable public abstract Long categoryId();
    @Nullable public abstract String label();
    @Nullable public abstract String comment();
    @Nullable public abstract Long accountId();

    static AutoFillData create(@Nullable Money amount, @Nullable Long categoryId, @Nullable String label, @Nullable String comment, @Nullable Long accountId) {
      return new AutoValue_LoadAutoFillDataTask_AutoFillData(amount, categoryId, label, comment, accountId);
    }
  }
}
