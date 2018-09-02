package org.totschnig.myexpenses.task;

import android.os.Bundle;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.provider.TransactionProvider;

public class AccountSortTask extends ExtraTask<Void> {
  AccountSortTask(TaskExecutionFragment taskExecutionFragment, int taskId) {
    super(taskExecutionFragment, taskId);
  }

  @Override
  protected Void doInBackground(Bundle... bundles) {
    MyApplication.getInstance().getContentResolver()
        .call(TransactionProvider.DUAL_URI, TransactionProvider.METHOD_SORT_ACCOUNTS, null, bundles[0]);
    return null;
  }
}
