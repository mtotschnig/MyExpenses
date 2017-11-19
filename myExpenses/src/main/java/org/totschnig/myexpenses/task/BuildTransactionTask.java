package org.totschnig.myexpenses.task;

import android.os.Bundle;

import org.apache.commons.lang3.NotImplementedException;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.ProviderUtils;

public class BuildTransactionTask extends ExtraTask<Transaction> {
  public static final String KEY_EXTRAS = "extras";
  BuildTransactionTask(TaskExecutionFragment taskExecutionFragment, int taskId) {
    super(taskExecutionFragment, taskId);
  }
  @Override
  protected Transaction doInBackground(Bundle... bundles) {
    try {
      return ProviderUtils.buildFromExtras(bundles[0].getBundle(KEY_EXTRAS));
    } catch (NotImplementedException e) {
      return null;
    }
  }
}
