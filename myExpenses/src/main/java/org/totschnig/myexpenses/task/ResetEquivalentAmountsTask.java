package org.totschnig.myexpenses.task;

import android.os.Bundle;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Result;

public class ResetEquivalentAmountsTask extends ExtraTask<Result<Integer>> {
  ResetEquivalentAmountsTask(TaskExecutionFragment taskExecutionFragment, int taskId) {
    super(taskExecutionFragment, taskId);
  }

  @Override
  protected Result<Integer> doInBackground(Bundle... bundles) {
    final Bundle result = MyApplication.getInstance()
        .getContentResolver()
        .call(TransactionProvider.DUAL_URI, TransactionProvider.METHOD_RESET_EQUIVALENT_AMOUNTS, null, null);
    return result == null ? Result.ofFailure(0) :
        Result.ofSuccess(0, result.getInt(TransactionProvider.KEY_RESULT));
  }
}
