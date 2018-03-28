package org.totschnig.myexpenses.task;

import android.os.Bundle;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.util.Result;

class SplitCommandTask extends ExtraTask<Result> {
  Result FAILURE = Result.ofFailure(R.string.split_transaction_error);
  SplitCommandTask(TaskExecutionFragment tTaskExecutionFragment, int taskId) {
    super(tTaskExecutionFragment, taskId);
  }

  @Override
  protected Result doInBackground(Bundle... bundles) {
    long[] ids = bundles[0].getLongArray(TaskExecutionFragment.KEY_LONG_IDS);
    if (ids == null) return FAILURE;
    final int count = ids.length;
    if (count == 0) return FAILURE;
    return SplitTransaction.split(ids);
  }
}