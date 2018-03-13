package org.totschnig.myexpenses.task;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.Bundle;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;

public class ResetEquivalentAmountsTask extends ExtraTask<Void> {
  ResetEquivalentAmountsTask(TaskExecutionFragment taskExecutionFragment, int taskId) {
    super(taskExecutionFragment, taskId);
  }

  @Override
  protected Void doInBackground(Bundle... bundles) {
    MyApplication application = MyApplication.getInstance();
    ContentResolver cr = application.getContentResolver();
    ContentValues values = new ContentValues();
    values.putNull(DatabaseConstants.KEY_EQUIVALENT_AMOUNT);
    cr.update(TransactionProvider.TRANSACTIONS_URI, values, null, null);
    return null;
  }
}
