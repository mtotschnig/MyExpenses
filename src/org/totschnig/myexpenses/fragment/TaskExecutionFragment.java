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

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INSTANCEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID;

import java.io.Serializable;
import java.util.Date;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.*;
import org.totschnig.myexpenses.model.Transaction.CrStatus;
import org.totschnig.myexpenses.provider.TransactionProvider;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

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
  public static final int TASK_DELETE_PAYMENT_METHODS = 8;
  public static final int TASK_DELETE_PAYEES = 9;
  public static final int TASK_DELETE_TEMPLATES = 10;
  public static final int TASK_TOGGLE_CRSTATUS = 11;
  public static final int TASK_MOVE = 12;
  public static final int TASK_NEW_FROM_TEMPLATE = 13;
  public static final int TASK_DELETE_CATEGORY = 14;
  public static final int TASK_NEW_PLAN = 15;
  public static final int TASK_NEW_CALENDAR = 16;
  public static final int TASK_CANCEL_PLAN_INSTANCE = 17;
  public static final int TASK_RESET_PLAN_INSTANCE = 18;

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
  public static TaskExecutionFragment newInstance(int taskId, Long[] objectIds, Serializable extra) {
    TaskExecutionFragment f = new TaskExecutionFragment();
    Bundle bundle = new Bundle();
    bundle.putInt("taskId", taskId);
    if (objectIds != null)
      bundle.putSerializable("objectIds", objectIds);
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
    int taskId = args.getInt("taskId");
    Log.i(MyApplication.TAG,"TaskExecutionFragment created for task "+taskId);
    mTask = new GenericTask(taskId,args.getSerializable("extra"));
    long objectId = args.getLong("objectId");
    if (objectId != 0)
      mTask.execute(args.getLong("objectId"));
    else
      mTask.execute((Long[]) args.getSerializable("objectIds"));
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
    protected Object doInBackground(Long... ids) {
      Transaction t;
      Long transactionId;
      Long[][] extraInfo2d;
      ContentResolver cr;
      int successCount=0;
      switch (mTaskId) {
      case TASK_CLONE:
        for (long id: ids) {
          t = Transaction.getInstanceFromDb(id);
          if (t != null && t.saveAsNew() != null)
            successCount++;
        }
        return successCount;
      case TASK_INSTANTIATE_TRANSACTION:
        t = Transaction.getInstanceFromDb(ids[0]);
        if (t != null && t instanceof SplitTransaction)
          ((SplitTransaction) t).prepareForEdit();
        return t;
      case TASK_INSTANTIATE_TEMPLATE:
        return Template.getInstanceFromDb(ids[0]);
      case TASK_INSTANTIATE_TRANSACTION_FROM_TEMPLATE:
        //when we are called from a notification,
        //the template could have been deleted in the meantime 
        //getInstanceFromTemplate should return null in that case
        return Transaction.getInstanceFromTemplate(ids[0]);
      case TASK_NEW_FROM_TEMPLATE:
        for (int i=0; i<ids.length; i++) {
          t = Transaction.getInstanceFromTemplate(ids[i]);
          if (t != null) {
            if (mExtra != null) {
              extraInfo2d = (Long[][]) mExtra;
              t.setDate(new Date(extraInfo2d[i][1]));
              t.originPlanInstanceId = extraInfo2d[i][0];
            }
            if (t.save()!=null) {
              successCount++;
            }
          }
        }
        return successCount;
      case TASK_REQUIRE_ACCOUNT:
        Account account;
        account = Account.getInstanceFromDb(0);
        if (account == null) {
          account = new Account(
              getString(R.string.app_name),
              0,
              getString(R.string.default_account_description)
          );
          account.save();
        }
      return account;
      case TASK_DELETE_TRANSACTION:
        for (long id: ids) {
          Transaction.delete(id);
        }
        return null;
      case TASK_DELETE_ACCOUNT:
        Account.delete(ids[0]);
        return null;
      case TASK_DELETE_PAYMENT_METHODS:
        for (long id: ids) {
          PaymentMethod.delete(id);
        }
        return null;
      case TASK_DELETE_PAYEES:
        for (long id: ids) {
          Payee.delete(id);
        }
        return null;
      case TASK_DELETE_CATEGORY:
        for (long id: ids) {
          Category.delete(id);
        }
        return null;
      case TASK_DELETE_TEMPLATES:
        for (long id: ids) {
          Template.delete(id);
        }
        return null;
      case TASK_TOGGLE_CRSTATUS:
        t = Transaction.getInstanceFromDb(ids[0]);
        if (t != null) {
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
        }
        return null;
      case TASK_MOVE:
        Transaction.move(ids[0],(Long) mExtra);
        return null;
      case TASK_NEW_PLAN:
        Uri uri = ((Plan)mExtra).save();
        return uri == null ? null : ContentUris.parseId(uri);
      case TASK_NEW_CALENDAR:
        return MyApplication.getInstance().createPlanner();
      case TASK_CANCEL_PLAN_INSTANCE:
        cr = MyApplication.getInstance().getContentResolver();
        for (int i=0; i<ids.length; i++) {
          extraInfo2d = (Long[][]) mExtra;
          transactionId = extraInfo2d[i][1];
          Long templateId = extraInfo2d[i][0];
          if (transactionId != null && transactionId >0L) {
            Transaction.delete(transactionId);
          } else {
            cr.delete(TransactionProvider.PLAN_INSTANCE_STATUS_URI,
              KEY_INSTANCEID + " = ?",
              new String[]{String.valueOf(ids[i])});
          }
          ContentValues values = new ContentValues();
          values.putNull(KEY_TRANSACTIONID);
          values.put(KEY_TEMPLATEID, templateId);
          values.put(KEY_INSTANCEID, ids[i]);
          cr.insert(TransactionProvider.PLAN_INSTANCE_STATUS_URI, values);
        }
        return null;
      case TASK_RESET_PLAN_INSTANCE:
        cr = MyApplication.getInstance().getContentResolver();
        for (int i=0; i<ids.length; i++) {
          transactionId = ((Long []) mExtra)[i];
          if (transactionId != null && transactionId >0L) {
            Transaction.delete(transactionId);
          }
          cr.delete(TransactionProvider.PLAN_INSTANCE_STATUS_URI,
              KEY_INSTANCEID + " = ?",
              new String[]{String.valueOf(ids[i])});
        }
        return null;
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