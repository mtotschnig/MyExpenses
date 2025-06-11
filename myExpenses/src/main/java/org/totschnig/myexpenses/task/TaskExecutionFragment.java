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
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.io.Serializable;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;


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
  private static final String KEY_RUNNING = "running";
  private static final String KEY_TASKID = "taskId";
  public static final String KEY_WITH_PARTIES = "withParties";
  public static final String KEY_WITH_CATEGORIES = "withCategories";
  public static final String KEY_FILE_PATH = "filePath";

  public static final int TASK_GRISBI_IMPORT = 19;
  public static final int TASK_REPAIR_PLAN = 41;

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

    crashHandler = MyApplication.Companion.getInstance().getAppComponent().crashHandler();

    // Create and execute the background task.
    Bundle args = getArguments();
    int taskId = args.getInt(KEY_TASKID);
    crashHandler.addBreadcrumb(String.valueOf(taskId));
    switch (taskId) {
      case TASK_GRISBI_IMPORT:
        new GrisbiImportTask(this, args).execute();
        break;
      default:
        try {
          new GenericTask<T>(this, taskId, args.getSerializable(KEY_EXTRA))
              .execute((T[]) args.getSerializable(KEY_OBJECT_IDS));
        } catch (ClassCastException e) {
          CrashHandler.report(e);
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