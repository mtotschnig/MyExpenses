package org.totschnig.myexpenses.task;

import android.os.Bundle;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Result;

public class CategoriesSetupTask extends ExtraTask<Result> {
  CategoriesSetupTask(TaskExecutionFragment taskExecutionFragment, int taskId) {
    super(taskExecutionFragment, taskId);
  }

  @Override
  protected Result doInBackground(Bundle... bundles) {
    int total = MyApplication.getInstance()
        .getContentResolver()
        .call(TransactionProvider.DUAL_URI, TransactionProvider.METHOD_SETUP_CATEGORIES, null, null)
        .getInt(TransactionProvider.KEY_RESULT);
    return total == 0 ? Result.ofSuccess(R.string.import_categories_none) :
        Result.ofSuccess(R.string.import_categories_success, null, String.valueOf(total));
  }
}
