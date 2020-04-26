package org.totschnig.myexpenses.task;

import android.os.AsyncTask;
import android.os.Bundle;

/**
 * {@link AsyncTask} that takes {@link Bundle} as parameter and communicates with {@link TaskExecutionFragment}
 */
abstract class ExtraTask<T> extends AsyncTask<Bundle, Void, T> {
  protected final TaskExecutionFragment taskExecutionFragment;
  private final int taskId;

  ExtraTask(TaskExecutionFragment taskExecutionFragment, int taskId) {
    this.taskExecutionFragment = taskExecutionFragment;
    this.taskId = taskId;
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
  protected void onPostExecute(T result) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onPostExecute(taskId, result);
    }
  }
}
