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

package org.totschnig.myexpenses.task;


import java.io.Serializable;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.activity.BackupRestoreActivity;
import org.totschnig.myexpenses.export.qif.QifDateFormat;
import org.totschnig.myexpenses.model.*;
import org.totschnig.myexpenses.model.Account.CurrencyEnum;
import org.totschnig.myexpenses.provider.DatabaseConstants;

import org.totschnig.myexpenses.export.qif.*;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

/**
 * This Fragment manages a single background task and retains itself across
 * configuration changes. It handles several task that each operate on a single
 * db object identified by its row id
 * 
 */
public class TaskExecutionFragment extends Fragment {
  private static final String KEY_RUNNING = "running";
  private static final String KEY_TASKID = "taskId";
  public static final String KEY_WITH_PARTIES = "withParties";
  public static final String KEY_WITH_CATEGORIES = "withCategories";
  public static final String KEY_WITH_TRANSACTIONS = "withTransactions";
  public static final String KEY_FILE_PATH = "filePath";
  public static final String KEY_EXTERNAL = "external";
  public static final String KEY_DATE_FORMAT = "dateFormat";

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
  public static final int TASK_GRISBI_IMPORT = 19;
  public static final int TASK_QIF_IMPORT = 20;
  public static final int TASK_EXPORT = 21;
  public static final int TASK_BACKUP = 22;
  public static final int TASK_RESTORE = 23;
  

  /**
   * Callback interface through which the fragment will report the task's
   * progress and results back to the Activity.
   */
  public static interface TaskCallbacks {
    void onPreExecute();

    void onProgressUpdate(Object progress);

    void onCancelled();

    /**
     * @param taskId
     *          with which TaskExecutionFragment was created
     * @param an
     *          object that the activity expects from the task, for example an
     *          instantiated DAO
     */
    void onPostExecute(int taskId, Object o);
  }

  TaskCallbacks mCallbacks;

  public static TaskExecutionFragment newInstance(int taskId, Long[] objectIds,
      Serializable extra) {
    TaskExecutionFragment f = new TaskExecutionFragment();
    Bundle bundle = new Bundle();
    bundle.putInt(KEY_TASKID, taskId);
    if (objectIds != null)
      bundle.putSerializable("objectIds", objectIds);
    if (extra != null)
      bundle.putSerializable("extra", extra);
    f.setArguments(bundle);
    return f;
  }

  public static TaskExecutionFragment newInstanceGrisbiImport(boolean external,
      String filePath, boolean withCategories, boolean withParties) {
    TaskExecutionFragment f = new TaskExecutionFragment();
    Bundle bundle = new Bundle();
    bundle.putInt(KEY_TASKID, TASK_GRISBI_IMPORT);
    bundle.putBoolean(KEY_EXTERNAL, external);
    bundle.putString(KEY_FILE_PATH, filePath);
    bundle.putBoolean(KEY_WITH_PARTIES, withParties);
    bundle.putBoolean(KEY_WITH_CATEGORIES, withCategories);
    f.setArguments(bundle);
    return f;
  }

  public static TaskExecutionFragment newInstanceQifImport(String filePath,
      QifDateFormat qifDateFormat, long accountId,
      String currency, boolean withTransactions, boolean withCategories, boolean withParties) {
    TaskExecutionFragment f = new TaskExecutionFragment();
    Bundle bundle = new Bundle();
    bundle.putInt(KEY_TASKID, TASK_QIF_IMPORT);
    bundle.putString(KEY_FILE_PATH, filePath);
    bundle.putSerializable(KEY_DATE_FORMAT, qifDateFormat);
    bundle.putLong(DatabaseConstants.KEY_ACCOUNTID, accountId);
    bundle.putString(DatabaseConstants.KEY_CURRENCY, currency);
    bundle.putBoolean(KEY_WITH_TRANSACTIONS, withTransactions);
    bundle.putBoolean(KEY_WITH_PARTIES, withParties);
    bundle.putBoolean(KEY_WITH_CATEGORIES, withCategories);
    f.setArguments(bundle);
    return f;
  }

  public static TaskExecutionFragment newInstanceExport(Bundle b) {
    TaskExecutionFragment f = new TaskExecutionFragment();
    b.putInt(KEY_TASKID, TASK_EXPORT);
    f.setArguments(b);
    return f;
  }

  public static TaskExecutionFragment newInstanceRestore(String fileName) {
    TaskExecutionFragment f = new TaskExecutionFragment();
    Bundle b = new Bundle();
    b.putInt(KEY_TASKID, TASK_RESTORE);
    b.putString(BackupRestoreActivity.KEY_FILENAME, fileName);
    f.setArguments(b);
    return f;
  }

  /**
   * Hold a reference to the parent Activity so we can report the task's current
   * progress and results. The Android framework will pass us a reference to the
   * newly created Activity after each configuration change.
   */
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    mCallbacks = (TaskCallbacks) activity;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(KEY_RUNNING, true);
  }

  /**
   * This method will only be called once when the retained Fragment is first
   * created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState != null
        && savedInstanceState.getBoolean(KEY_RUNNING, true)) {
      // if we are recreated, prevent the task from being executed twice
      if (mCallbacks != null) {
        mCallbacks.onCancelled();
      }
      return;
    }
    // Retain this fragment across configuration changes.
    setRetainInstance(true);

    // Create and execute the background task.
    Bundle args = getArguments();
    int taskId = args.getInt(KEY_TASKID);
    Log.i(MyApplication.TAG, "TaskExecutionFragment created for task " + taskId);
    try {
      switch (taskId) {
      case TASK_GRISBI_IMPORT:
        new GrisbiImportTask(this, args).execute();
        break;
      case TASK_QIF_IMPORT:
        new QifImportTask(this, args).execute();
        break;
      case TASK_EXPORT:
        new ExportTask(this,args).execute();
        break;
      case TASK_RESTORE:
        new RestoreTask(this).execute(args.getString(BackupRestoreActivity.KEY_FILENAME));
        break;
      default:
        new GenericTask(this, taskId, args.getSerializable("extra"))
            .execute((Long[]) args.getSerializable("objectIds"));
      }
    } catch (ClassCastException e) {
      // the cast could fail, if Fragment is recreated,
      // but we are cancelling above in that case
      mCallbacks.onCancelled();
    }
  }

  /**
   * Set the callback to null so we don't accidentally leak the Activity
   * instance.
   */
  @Override
  public void onDetach() {
    super.onDetach();
    mCallbacks = null;
  }
}