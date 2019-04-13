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

import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.util.HashMap;

/**
 * This Fragment manages a single background task and retains
 * itself across configuration changes.
 * It handles saving model objects to the database
 * It calls getObject on its callback to retrieve the object
 * and calls save on the Object
 * it can return either the uri for the new object (null on failure)
 * or the number of stored objects in the db for the Model (error code < 0 on failure)
 * the later is only implemented for transactions
 */
@Deprecated
public class DbWriteFragment extends Fragment {

  private static final String KEY_RUNNING = "running";
  public static final int ERROR_UNKNOWN = -1;
  public static final int ERROR_EXTERNAL_STORAGE_NOT_AVAILABLE = -2;
  public static final int ERROR_PICTURE_SAVE_UNKNOWN = -3;
  public static final int ERROR_CALENDAR_INTEGRATION_NOT_AVAILABLE = -4;


  /**
   * Callback interface through which the fragment will report the
   * task's progress and results back to the Activity.
   */
  public interface TaskCallbacks {
    /**
     * @return get the Object that should be saved to DB
     */
    Model getObject();
    void onCancelled();
    /**
     * @param result normally the URI is returned to the calling activity,
     * optionally, when DbWriteFragment is created with returnSequenceCount true,
     * the sequence count from the table is returned instead
     */
    void onPostExecute(Object result);
  }
 
  private TaskCallbacks mCallbacks;
  private GenericWriteTask mTask;
  public static DbWriteFragment newInstance(boolean returnSequenceCount) {
    DbWriteFragment f = new DbWriteFragment();
    Bundle bundle = new Bundle();
    bundle.putBoolean("returnSequenceCount", returnSequenceCount);
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
    if (savedInstanceState != null
        && savedInstanceState.getBoolean(KEY_RUNNING, true)) {
      // if we are recreated, prevent the task from being executed twice
      return;
    }
    // Retain this fragment across configuration changes.
    setRetainInstance(true);
 
    // Create and execute the background task.
    Bundle args = getArguments();
    mTask = new GenericWriteTask(args.getBoolean("returnSequenceCount", false));
    mTask.execute(mCallbacks.getObject());
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

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(KEY_RUNNING, true);
  }
 
  /**
   *
   * Note that we need to check if the callbacks are null in each
   * method in case they are invoked after the Activity's and
   * Fragment's onDestroy() method have been called.
   */
  private class GenericWriteTask extends AsyncTask<Model, Void, Object> {
    boolean returnSequenceCount;
    public GenericWriteTask(boolean returnSequenceCount) {
      this.returnSequenceCount = returnSequenceCount;
    }

    @Override
    protected void onPreExecute() {
    }
 
    /**
     * Note that we do NOT call the callback object's methods
     * directly from the background thread, as this could result
     * in a race condition.
     */
    @Override
    protected Object doInBackground(Model... object) {
      long error = ERROR_UNKNOWN;
      if (object[0] == null) {
        CrashHandler.report("DbWriteFragment called from an activity that did not provide an object");
        return null;
      }
      Uri uri = null;

      try {
        uri = object[0].save();
      } catch (Transaction.ExternalStorageNotAvailableException e) {
        error = ERROR_EXTERNAL_STORAGE_NOT_AVAILABLE;
      } catch (Transaction.UnknownPictureSaveException e) {
        HashMap<String, String> customData = new HashMap<>();
        customData.put("pictureUri", e.pictureUri.toString());
        customData.put("homeUri", e.homeUri.toString());
        CrashHandler.report(e, customData);
        error = ERROR_PICTURE_SAVE_UNKNOWN;
      } catch (Plan.CalendarIntegrationNotAvailableException e) {
        error = ERROR_CALENDAR_INTEGRATION_NOT_AVAILABLE;
      } catch (Exception e) {
        CrashHandler.report(e);
      }
      if (returnSequenceCount && object[0] instanceof Transaction)
        return uri == null ? error : Transaction.getSequenceCount();
      else
        return uri;
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

    //TODO refactor so that result is stored if onPostExecute is called while callbacks are detached
    @Override
    protected void onPostExecute(Object result) {
      if (mCallbacks != null) {
        mCallbacks.onPostExecute(result);
      }
    }
  }
}