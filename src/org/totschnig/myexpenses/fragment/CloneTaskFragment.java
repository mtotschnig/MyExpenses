package org.totschnig.myexpenses.fragment;

import org.totschnig.myexpenses.model.Transaction;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;

/**
 * This Fragment manages a single background task and retains
 * itself across configuration changes.
 */
public class CloneTaskFragment extends Fragment {
  Long id;
  /**
   * Callback interface through which the fragment will report the
   * task's progress and results back to the Activity.
   */
  public static interface TaskCallbacks {
    void onPreExecute();
    void onProgressUpdate(int percent);
    void onCancelled();
    void onPostExecute();
  }
 
  private TaskCallbacks mCallbacks;
  private CloneTask mTask;
  public static CloneTaskFragment newInstance(long transactionId) {
    CloneTaskFragment f = new CloneTaskFragment();
    Bundle bundle = new Bundle();
    bundle.putLong("id", transactionId);
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
    mTask = new CloneTask();
    mTask.execute(getArguments().getLong("id"));
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
   * A dummy task that performs some (dumb) background work and
   * proxies progress updates and results back to the Activity.
   *
   * Note that we need to check if the callbacks are null in each
   * method in case they are invoked after the Activity's and
   * Fragment's onDestroy() method have been called.
   */
  private class CloneTask extends AsyncTask<Long, Void, Void> {
 
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
    protected Void doInBackground(Long... id) {
      Transaction.getInstanceFromDb(id[0]).saveAsNew();
      return null;
    }
 
    @Override
    protected void onProgressUpdate(Void... ignore) {
    }
 
    @Override
    protected void onCancelled() {
      if (mCallbacks != null) {
        mCallbacks.onCancelled();
      }
    }
 
    @Override
    protected void onPostExecute(Void ignore) {
      if (mCallbacks != null) {
        mCallbacks.onPostExecute();
      }
    }
  }
}