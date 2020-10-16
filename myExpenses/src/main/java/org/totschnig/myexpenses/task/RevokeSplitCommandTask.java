package org.totschnig.myexpenses.task;

import android.os.Bundle;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.util.Result;

import static org.totschnig.myexpenses.util.Result.FAILURE;

class RevokeSplitCommandTask extends ExtraTask<Result> {
  RevokeSplitCommandTask(TaskExecutionFragment tTaskExecutionFragment, int taskId) {
    super(tTaskExecutionFragment, taskId);
  }

  @Override
  protected Result doInBackground(Bundle... bundles) {
    long[] ids = bundles[0].getLongArray(TaskExecutionFragment.KEY_LONG_IDS);
    if (ids == null) return FAILURE;
    final int count = ids.length;
    if (count == 0) return FAILURE;
    int success = 0;
    for (long id: ids ) {
      Transaction split = Transaction.getInstanceFromDb(id);
      if (split instanceof SplitTransaction) {
        if (((SplitTransaction) split).unsplit()) {
          success++;
        }
      }
    }
    if (success == count) {
      return Result.ofSuccess(taskExecutionFragment.requireContext().getResources().getQuantityString(R.plurals.ungroup_split_transaction_success, count, count));
    }
    return Result.ofFailure(R.string.ungroup_split_transaction_failure, count - success, count);
  }
}