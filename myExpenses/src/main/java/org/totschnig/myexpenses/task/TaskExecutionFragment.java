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


import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.export.qif.QifDateFormat;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.io.Serializable;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import timber.log.Timber;


/**
 * This Fragment manages a single background task and retains itself across
 * configuration changes. It handles several task that each operate on a single
 * db object identified by its row id
 */
@Deprecated
public class TaskExecutionFragment<T> extends Fragment {
  private static final String KEY_EXTRA = "extra";
  @Deprecated
  public static final String KEY_OBJECT_IDS = "objectIds";
  public static final String KEY_LONG_IDS = "longIds";
  private static final String KEY_RUNNING = "running";
  private static final String KEY_TASKID = "taskId";
  public static final String KEY_WITH_PARTIES = "withParties";
  public static final String KEY_WITH_CATEGORIES = "withCategories";
  public static final String KEY_WITH_TRANSACTIONS = "withTransactions";
  public static final String KEY_FILE_PATH = "filePath";
  public static final String KEY_DATE_FORMAT = "dateFormat";
  public static final String KEY_ENCODING = "encoding";
  public static final String KEY_FORMAT = "format";

  public static final int TASK_DELETE_TRANSACTION = 6;
  public static final int TASK_DELETE_ACCOUNT = 7;
  public static final int TASK_DELETE_PAYMENT_METHODS = 8;
  public static final int TASK_DELETE_PAYEES = 9;
  public static final int TASK_DELETE_TEMPLATES = 10;
  public static final int TASK_TOGGLE_CRSTATUS = 11;
  public static final int TASK_MOVE = 12;
  public static final int TASK_NEW_FROM_TEMPLATE = 13;
  public static final int TASK_DELETE_CATEGORY = 14;
  public static final int TASK_INSTANTIATE_PLAN = 15;
  public static final int TASK_CANCEL_PLAN_INSTANCE = 17;
  public static final int TASK_RESET_PLAN_INSTANCE = 18;
  public static final int TASK_GRISBI_IMPORT = 19;
  public static final int TASK_QIF_IMPORT = 20;
  public static final int TASK_EXPORT = 21;
  public static final int TASK_BACKUP = 22;
  public static final int TASK_RESTORE = 23;
  public static final int TASK_BALANCE = 24;
  public static final int TASK_PRINT = 25;
  public static final int TASK_UPDATE_SORT_KEY = 27;
  public static final int TASK_SET_EXCLUDE_FROM_TOTALS = 29;
  public static final int TASK_SPLIT = 30;
  public static final int TASK_REVOKE_SPLIT = 31;
  public static final int TASK_DELETE_IMAGES = 32;
  public static final int TASK_SAVE_IMAGES = 33;
  public static final int TASK_UNDELETE_TRANSACTION = 34;
  public static final int TASK_EXPORT_CATEGORIES = 35;
  public static final int TASK_MOVE_CATEGORY = 38;
  public static final int TASK_SWAP_SORT_KEY = 39;
  public static final int TASK_MOVE_UNCOMMITED_SPLIT_PARTS = 40;
  public static final int TASK_REPAIR_PLAN = 41;
  public static final int TASK_WEBDAV_TEST_LOGIN = 42;
  public static final int TASK_CREATE_SYNC_ACCOUNT = 43;
  public static final int TASK_SYNC_UNLINK = 44;
  public static final int TASK_SYNC_LINK_LOCAL = 45;
  public static final int TASK_SYNC_LINK_REMOTE = 46;
  public static final int TASK_SYNC_REMOVE_BACKEND = 47;
  public static final int TASK_SYNC_LINK_SAVE = 48;
  /**
   * verify if a given uuid exists in a given backend
   */
  public static final int TASK_SYNC_CHECK = 49;

  public static final int TASK_FETCH_SYNC_ACCOUNT_DATA = 51;
  public static final int TASK_SETUP_FROM_SYNC_ACCOUNTS = 52;
  public static final int TASK_REPAIR_SYNC_BACKEND = 53;
  public static final int TASK_STORE_SETTING = 54;
  public static final int TASK_VALIDATE_LICENCE = 55;
  public static final int TASK_REMOVE_LICENCE = 56;
  public static final int TASK_BUILD_TRANSACTION_FROM_INTENT_EXTRAS = 57;
  public static final int TASK_RESET_EQUIVALENT_AMOUNTS = 59;
  public static final int TASK_ACCOUNT_SORT = 60;
  public static final int TASK_CATEGORY_COLOR = 61;
  public static final int TASK_SETUP_CATEGORIES = 62;
  public static final int TASK_SET_ACCOUNT_HIDDEN = 63;
  public static final int TASK_SET_ACCOUNT_SEALED = 64;


  /**
   * Callback interface through which the fragment will report the task's
   * progress and results back to the Activity.
   */
  public interface TaskCallbacks {
    void onPreExecute();

    void onProgressUpdate(Object progress);

    void onCancelled();

    /**
     * @param taskId with which TaskExecutionFragment was created
     * @param o      object that the activity expects from the task, for example an
     *               instantiated DAO
     */
    void onPostExecute(int taskId, Object o);
  }

  protected CrashHandler crashHandler;

  //TODO refactor so that callbacks are not visible to hosted tasks
  TaskCallbacks mCallbacks;

  @Deprecated
  public static <T> TaskExecutionFragment<T> newInstance(int taskId, T[] objectIds,
                                                      Serializable extra) {
    TaskExecutionFragment<T> f = new TaskExecutionFragment<>();
    Bundle bundle = new Bundle();
    bundle.putInt(KEY_TASKID, taskId);
    if (objectIds != null) {
      //TODO would be safer to use putLongArray/putStringArray
      bundle.putSerializable(KEY_OBJECT_IDS, objectIds);
    }
    if (extra != null) {
      bundle.putSerializable(KEY_EXTRA, extra);
    }
    f.setArguments(bundle);
    return f;
  }

  public static TaskExecutionFragment newInstanceGrisbiImport(Uri mUri, boolean withCategories, boolean withParties) {
    TaskExecutionFragment f = new TaskExecutionFragment();
    Bundle bundle = new Bundle();
    bundle.putInt(KEY_TASKID, TASK_GRISBI_IMPORT);
    bundle.putParcelable(KEY_FILE_PATH, mUri);
    bundle.putBoolean(KEY_WITH_PARTIES, withParties);
    bundle.putBoolean(KEY_WITH_CATEGORIES, withCategories);
    f.setArguments(bundle);
    return f;
  }

  public static TaskExecutionFragment newInstanceQifImport(
      Uri mUri, QifDateFormat qifDateFormat, long accountId, CurrencyUnit currency,
      boolean withTransactions, boolean withCategories, boolean withParties,
      String encoding) {
    TaskExecutionFragment f = new TaskExecutionFragment();
    Bundle bundle = new Bundle();
    bundle.putInt(KEY_TASKID, TASK_QIF_IMPORT);
    bundle.putParcelable(KEY_FILE_PATH, mUri);
    bundle.putSerializable(KEY_DATE_FORMAT, qifDateFormat);
    bundle.putLong(DatabaseConstants.KEY_ACCOUNTID, accountId);
    bundle.putSerializable(DatabaseConstants.KEY_CURRENCY, currency);
    bundle.putBoolean(KEY_WITH_TRANSACTIONS, withTransactions);
    bundle.putBoolean(KEY_WITH_PARTIES, withParties);
    bundle.putBoolean(KEY_WITH_CATEGORIES, withCategories);
    bundle.putString(KEY_ENCODING, encoding);
    f.setArguments(bundle);
    return f;
  }

  public static TaskExecutionFragment newInstanceWithBundle(@NonNull Bundle b, int taskId) {
    TaskExecutionFragment f = new TaskExecutionFragment<>();
    b.putInt(KEY_TASKID, taskId);
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
      return;
    }
    // Retain this fragment across configuration changes.
    setRetainInstance(true);

    crashHandler = MyApplication.getInstance().getAppComponent().crashHandler();

    // Create and execute the background task.
    Bundle args = getArguments();
    int taskId = args.getInt(KEY_TASKID);
    crashHandler.addBreadcrumb(String.valueOf(taskId));
    switch (taskId) {
      case TASK_GRISBI_IMPORT:
        new GrisbiImportTask(this, args).execute();
        break;
      case TASK_QIF_IMPORT:
        new QifImportTask(this, args).execute();
        break;
      case TASK_EXPORT:
        new ExportTask(this, args).execute();
        break;
      case TASK_RESTORE:
        new RestoreTask(this, args).execute();
        break;
      case TASK_PRINT:
        new PrintTask(this, args).execute();
        break;
      case TASK_WEBDAV_TEST_LOGIN:
        new TestLoginTask(this, args).execute();
        break;
      case TASK_CREATE_SYNC_ACCOUNT:
        new SyncAccountTask(this, args, true).execute();
        break;
      case TASK_FETCH_SYNC_ACCOUNT_DATA:
        new SyncAccountTask(this, args, false).execute();
        break;
      case TASK_VALIDATE_LICENCE:
        new LicenceApiTask(this, taskId).execute();
        break;
      case TASK_REMOVE_LICENCE:
        new LicenceApiTask(this, taskId).execute();
        break;
      case TASK_BUILD_TRANSACTION_FROM_INTENT_EXTRAS:
        new BuildTransactionTask(this, taskId).execute(args);
        break;
      case TASK_RESET_EQUIVALENT_AMOUNTS:
        new ResetEquivalentAmountsTask(this, taskId).execute(args);
        break;
      case TASK_SPLIT:
        new SplitCommandTask(this, taskId).execute(args);
        break;
      case TASK_REVOKE_SPLIT:
        new RevokeSplitCommandTask(this, taskId).execute(args);
        break;
      case TASK_ACCOUNT_SORT:
        new AccountSortTask(this, taskId).execute(args);
        break;
      case TASK_SETUP_CATEGORIES:
        new CategoriesSetupTask(this, taskId).execute(args);
        break;

      default:
        try {
          new GenericTask<T>(this, taskId, args.getSerializable(KEY_EXTRA))
              .execute((T[]) args.getSerializable(KEY_OBJECT_IDS));
        } catch (ClassCastException e) {
          Timber.e(e);
        }
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