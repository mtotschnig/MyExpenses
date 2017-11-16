package org.totschnig.myexpenses.task;

import android.os.AsyncTask;
import android.os.Bundle;

import org.apache.commons.lang3.NotImplementedException;
import org.totschnig.myexpenses.provider.ProviderUtils;

class ExtraTask extends AsyncTask<Bundle, Void, Object> {
  private final TaskExecutionFragment taskExecutionFragment;
  private final int taskId;

  ExtraTask(TaskExecutionFragment taskExecutionFragment, int taskId) {
    this.taskExecutionFragment = taskExecutionFragment;
    this.taskId = taskId;
  }

  @Override
  protected Object doInBackground(Bundle... bundles) {
    switch (taskId) {
      case TaskExecutionFragment.TASK_BUILD_TRANSACTION_FROM_INTENT_EXTRAS: {
        try {
          return ProviderUtils.buildFromExtras(bundles[0]);
        } catch (NotImplementedException ignored) {}
      }
    }
    return null;
  }

  @Override
  protected void onPreExecute() {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onPreExecute();
    }
  }

  @Override
  protected void onCancelled() {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onCancelled();
    }
  }

  @Override
  protected void onPostExecute(Object result) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onPostExecute(taskId, result);
    }
  }
}
