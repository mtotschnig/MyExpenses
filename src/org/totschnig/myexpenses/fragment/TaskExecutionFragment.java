/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
*/
//based on http://www.androiddesignpatterns.com/2013/04/retaining-objects-across-config-changes.html

package org.totschnig.myexpenses.fragment;

import java.io.Serializable;
import java.util.TimeZone;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.model.*;
import org.totschnig.myexpenses.model.Transaction.CrStatus;
import org.totschnig.myexpenses.provider.DbUtils;

import com.android.calendar.CalendarContractCompat.Events;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;

/**
 * This Fragment manages a single background task and retains
 * itself across configuration changes.
 * It handles several task that each operate on a single
 * db object identified by its row id
 * 
 */
public class TaskExecutionFragment extends Fragment {
  public static final int TASK_CLONE = 1;
  public static final int TASK_INSTANTIATE_TRANSACTION = 2;
  public static final int TASK_INSTANTIATE_TEMPLATE = 3;
  public static final int TASK_INSTANTIATE_TRANSACTION_FROM_TEMPLATE = 4;
  public static final int TASK_REQUIRE_ACCOUNT = 5;
  public static final int TASK_DELETE_TRANSACTION = 6;
  public static final int TASK_DELETE_ACCOUNT = 7;
  public static final int TASK_DELETE_PAYMENT_METHOD = 8;
  public static final int TASK_DELETE_PAYEE = 9;
  public static final int TASK_DELETE_TEMPLATE = 10;
  public static final int TASK_TOGGLE_CRSTATUS = 11;
  public static final int TASK_MOVE = 12;
  public static final int TASK_NEW_FROM_TEMPLATE = 13;
  public static final int TASK_DELETE_CATEGORY = 14;
  public static final int TASK_NEW_PLAN = 15;

  /**
   * Callback interface through which the fragment will report the
   * task's progress and results back to the Activity.
   */
  public static interface TaskCallbacks {
    void onPreExecute();
    void onProgressUpdate(int percent);
    void onCancelled();
    /**
     * @param taskId with which TaskExecutionFragment was created
     * @param an object that the activity expects from the task, for example an instantiated
     * DAO
     */
    void onPostExecute(int taskId,Object o);
  }

  private TaskCallbacks mCallbacks;
  private GenericTask mTask;
  public static TaskExecutionFragment newInstance(int taskId, Long objectId, Serializable extra) {
    TaskExecutionFragment f = new TaskExecutionFragment();
    Bundle bundle = new Bundle();
    bundle.putInt("taskId", taskId);
    if (objectId != null)
      bundle.putLong("objectId", objectId);
    if (extra != null)
      bundle.putSerializable("extra", extra);
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
    mTask = new GenericTask(args.getInt("taskId"),args.getSerializable("extra"));
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
  private class GenericTask extends AsyncTask<Long, Void, Object> {
    private int mTaskId;
    private Serializable mExtra;
    public GenericTask(int taskId,Serializable extra) {
      mTaskId = taskId;
      mExtra = extra;
    }

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
      Transaction t;
      switch (mTaskId) {
      case TASK_CLONE:
        Transaction.getInstanceFromDb(id[0]).saveAsNew();
        return null;
      case TASK_INSTANTIATE_TRANSACTION:
         t = Transaction.getInstanceFromDb(id[0]);
        if (t instanceof SplitTransaction)
          ((SplitTransaction) t).prepareForEdit();
        return t;
      case TASK_INSTANTIATE_TEMPLATE:
        return Template.getInstanceFromDb(id[0]);
      case TASK_INSTANTIATE_TRANSACTION_FROM_TEMPLATE:
        return Transaction.getInstanceFromTemplate(id[0]);
      case TASK_NEW_FROM_TEMPLATE:
        Transaction.getInstanceFromTemplate(id[0]).save();
        return null;
      case TASK_REQUIRE_ACCOUNT:
        Account account;
        try {
          account = Account.getInstanceFromDb(0);
        } catch (DataObjectNotFoundException e) {
          account = new Account(
              getString(R.string.app_name),
              0,
              getString(R.string.default_account_description)
          );
          account.save();
        }
      return account;
      case TASK_DELETE_TRANSACTION:
        Transaction.delete(id[0]);
        return null;
      case TASK_DELETE_ACCOUNT:
        Account.delete(id[0]);
        return null;
      case TASK_DELETE_PAYMENT_METHOD:
        PaymentMethod.delete(id[0]);
        return null;
      case TASK_DELETE_PAYEE:
        Payee.delete(id[0]);
        return null;
      case TASK_DELETE_CATEGORY:
        Category.delete(id[0]);
        return null;
      case TASK_DELETE_TEMPLATE:
        Template.delete(id[0]);
        return null;
      case TASK_TOGGLE_CRSTATUS:
        t = Transaction.getInstanceFromDb(id[0]);
        switch (t.crStatus) {
        case CLEARED:
          t.crStatus = CrStatus.RECONCILED;
          break;
        case RECONCILED:
          t.crStatus = CrStatus.UNRECONCILED;
          break;
        case UNRECONCILED:
          t.crStatus = CrStatus.CLEARED;
          break;
        }
        t.save();
        return null;
      case TASK_MOVE:
        Transaction.move(id[0],(Long) mExtra);
        return null;
      case TASK_NEW_PLAN:
        return Plan.create(id[0],(String)mExtra);
      }
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
        mCallbacks.onPostExecute(mTaskId,result);
      }
    }
  }
}