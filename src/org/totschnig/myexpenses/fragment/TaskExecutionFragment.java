//based on http://www.androiddesignpatterns.com/2013/04/retaining-objects-across-config-changes.html
package org.totschnig.myexpenses.fragment;

import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;

/**
 * This Fragment manages a single background task and retains
 * itself across configuration changes.
 * It handles several task that each operate on a single
 * db object identified by its row id
 */
public class TaskExecutionFragment extends Fragment {
  public static final int TASK_CLONE = 1;
  public static final int TASK_INSTANTIATE_TRANSACTION = 2;
  public static final int TASK_INSTANTIATE_TEMPLATE = 3;
  public static final int TASK_INSTANTIATE_TRANSACTION_FROM_TEMPLATE = 4;
  
  /**
   * Callback interface through which the fragment will report the
   * task's progress and results back to the Activity.
   */
  public static interface TaskCallbacks {
    void onPreExecute();
    void onProgressUpdate(int percent);
    void onCancelled();
    void onPostExecute(Object o);
  }
 
  private TaskCallbacks mCallbacks;
  private AbstractTask mTask;
  public static TaskExecutionFragment newInstance(int taskId, Long objectId) {
    TaskExecutionFragment f = new TaskExecutionFragment();
    Bundle bundle = new Bundle();
    bundle.putInt("taskId", taskId);
    bundle.putLong("objectId", objectId);
    f.setArguments(bundle);
    return f;
  }

  /**
   * Hold a reference to the parent Activity so we can report the
   * task's current progress and results. The Android framework
   * will pass us a reference to the newly created Activity after
   * each configuration change.
   */
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    mCallbacks = (TaskCallbacks) activity;
  }
 
  /**
   * This method will only be called once when the retained
   * Fragment is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
 
    // Retain this fragment across configuration changes.
    setRetainInstance(true);
 
    // Create and execute the background task.
    Bundle args = getArguments();
    switch (args.getInt("taskId")) {
    case TASK_CLONE:
      mTask = new CloneTask();
      break;
    case TASK_INSTANTIATE_TRANSACTION:
      mTask = new InstantiateTransactionTask();
      break;
    case TASK_INSTANTIATE_TEMPLATE:
      mTask = new InstantiateTemplateTask();
      break;
    case TASK_INSTANTIATE_TRANSACTION_FROM_TEMPLATE:
      mTask = new InstantiateTransactionFromTemplateTask();
      break;
    }
    mTask.execute(args.getLong("objectId"));
  }
 
  /**
   * Set the callback to null so we don't accidentally leak the
   * Activity instance.
   */
  @Override
  public void onDetach() {
    super.onDetach();
    mCallbacks = null;
  }
 
  /**
   *
   * Note that we need to check if the callbacks are null in each
   * method in case they are invoked after the Activity's and
   * Fragment's onDestroy() method have been called.
   */
  private abstract class AbstractTask extends AsyncTask<Long, Void, Object> {
 
    @Override
    protected void onPreExecute() {
      if (mCallbacks != null) {
        mCallbacks.onPreExecute();
      }
    }
 
    /**
     * Note that we do NOT call the callback object's methods
     * directly from the background thread, as this could result
     * in a race condition.
     */
    @Override
    protected Object doInBackground(Long... id) {
      return null;
    }
    @Override
    protected void onProgressUpdate(Void... ignore) {
/*      if (mCallbacks != null) {
        mCallbacks.onProgressUpdate(ignore[0]);
      }*/
    }
 
    @Override
    protected void onCancelled() {
      if (mCallbacks != null) {
        mCallbacks.onCancelled();
      }
    }
 
    @Override
    protected void onPostExecute(Object result) {
      if (mCallbacks != null) {
        mCallbacks.onPostExecute(result);
      }
    }
  }
  private class CloneTask extends AbstractTask {
    @Override
    protected Object doInBackground(Long... id) {
      Transaction.getInstanceFromDb(id[0]).saveAsNew();
      return null;
    }
  }
  private class InstantiateTransactionTask extends AbstractTask {
    @Override
    protected Object doInBackground(Long... id) {
      Transaction t = Transaction.getInstanceFromDb(id[0]);
      if (t instanceof SplitTransaction)
        ((SplitTransaction) t).prepareForEdit();
      return t;
    }
  }
  private class InstantiateTemplateTask extends AbstractTask {
    @Override
    protected Object doInBackground(Long... id) {
      return Template.getInstanceFromDb(id[0]);
    }
  }
  private class InstantiateTransactionFromTemplateTask extends AbstractTask {
    @Override
    protected Object doInBackground(Long... id) {
      return Transaction.getInstanceFromTemplate(id[0]);
    }
  }
}