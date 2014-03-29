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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.GrisbiImport;
import org.totschnig.myexpenses.dialog.GrisbiSourcesDialogFragment;
import org.totschnig.myexpenses.model.*;
import org.totschnig.myexpenses.model.Transaction.CrStatus;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.CategoryTree;
import org.totschnig.myexpenses.util.GrisbiHandler;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.res.Resources.NotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

/**
 * This Fragment manages a single background task and retains
 * itself across configuration changes.
 * It handles several task that each operate on a single
 * db object identified by its row id
 * 
 */
public class TaskExecutionFragment extends Fragment {
  private static final String KEY_RUNNING = "running";
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
  public static TaskExecutionFragment newInstanceGrisbiImport(boolean external, boolean withParties) {
    TaskExecutionFragment f = new TaskExecutionFragment();
    Bundle bundle = new Bundle();
    bundle.putInt("taskId", TASK_GRISBI_IMPORT);
    bundle.putBoolean("external", external);
    bundle.putBoolean("withParties", withParties);
    f.setArguments(bundle);
    return f;
  }
  public static TaskExecutionFragment newInstanceQifImport(String filePath, int dateFormat,
      long accountId) {
    TaskExecutionFragment f = new TaskExecutionFragment();
    Bundle bundle = new Bundle();
    bundle.putInt("taskId", TASK_QIF_IMPORT);
    bundle.putString("filePath", filePath);
    bundle.putInt("dateFormat", dateFormat);
    bundle.putLong("accountId", accountId);
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
  @Override
  public void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      outState.putBoolean(KEY_RUNNING,true);
  }

  /**
   * This method will only be called once when the retained
   * Fragment is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState!=null && savedInstanceState.getBoolean(KEY_RUNNING, true)) {
      //if we are recreated, prevent the task from being executed twice
      if (mCallbacks != null) {
        mCallbacks.onCancelled();
      }
      return;
    }
    // Retain this fragment across configuration changes.
    setRetainInstance(true);

    // Create and execute the background task.
    Bundle args = getArguments();
    int taskId = args.getInt("taskId");
    Log.i(MyApplication.TAG,"TaskExecutionFragment created for task "+taskId);
    try {
      switch(taskId) {
      case TASK_GRISBI_IMPORT:
        new GrisbiImportTask(args.getBoolean("withParties"))
          .execute(args.getBoolean("external"));
        break;
      case TASK_QIF_IMPORT:
        new QifImportTask(args.getInt("dateFormat"),args.getLong("accountId"))
          .execute(args.getString("filePath"));
        break;
      default:
        new GenericTask(taskId,args.getSerializable("extra"))
          .execute((Long[]) args.getSerializable("objectIds"));
      }
    } catch (ClassCastException e) {
      //the cast could fail, if Fragment is recreated,
      //but we are cancelling above in that case
      mCallbacks.onCancelled();
    }
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
  
  public class GrisbiImportTask extends AsyncTask<Boolean, Integer, Result> {
    
    public GrisbiImportTask(boolean withPartiesP) {
      this.withPartiesP = withPartiesP;
    }
    String title;
    private int max;
    /**
     * should we handle parties as well?
     */
    boolean withPartiesP;
    /**
     * this is set when we finish one phase (parsing, importing categories, importing parties)
     * so that we can adapt progress dialog in onProgressUpdate
     */
    boolean phaseChangedP = false;
    private CategoryTree catTree;
    private ArrayList<String> partiesList;

    public void setTitle(String title) {
      this.title = title;
    }
    public String getTitle() {
      return title;
    }

    /**
     * return false upon problem (and sets a result object) or true
     * @param source2 
     */
    protected Result parseXML(String sourceStr) {
      InputStream catXML = null;
      Result result;

      try {
        if (sourceStr.equals(GrisbiSourcesDialogFragment.IMPORT_SOURCE_INTERNAL)) {
          try {
            catXML = getResources().openRawResource(GrisbiSourcesDialogFragment.defaultSourceResId);
          } catch (NotFoundException e) {
            catXML = getResources().openRawResource(R.raw.cat_en);
          }
        } else {
          catXML = new FileInputStream(sourceStr);
        }
        result = Utils.analyzeGrisbiFileWithSAX(catXML);
        if (result.success) {
          catTree = (CategoryTree) result.extra[0];
          partiesList = (ArrayList<String>) result.extra[1];
        }
      } catch (FileNotFoundException e) {
        result = new Result(false,R.string.parse_error_file_not_found,sourceStr);
      } finally {
        if (catXML!=null) {
          try {
            catXML.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
      return result;
    }

    /**
     * made public to allow passing task to  {@link Utils#importCats(CategoryTree, GrisbiImportTask)}
     * and {@link Utils#importParties(ArrayList, GrisbiImportTask)}
     * @param i
     */
    public void publishProgress(Integer i) {
      super.publishProgress(i);
    }

    /* (non-Javadoc)
     * updates the progress dialog
     * @see android.os.AsyncTask#onProgressUpdate(Progress[])
     */
    protected void onProgressUpdate(Integer... values) {
      if (mCallbacks!=null) {
        if (phaseChangedP) {
          ((GrisbiImport) mCallbacks).setProgressMax(getMax());
          ((GrisbiImport) mCallbacks).setProgressTitle(getTitle());
          phaseChangedP = false;
        }
        mCallbacks.onProgressUpdate(values[0]);
      }
    }
    /* (non-Javadoc)
     * reports on success (with total number of imported categories) or failure
     * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
     */
    @Override
    protected void onPostExecute(Result result) {
      if (mCallbacks != null) {
        mCallbacks.onPostExecute(TASK_GRISBI_IMPORT,result);
      }
    }

    /* (non-Javadoc)
     * this is where the bulk of the work is done via calls to {@link #importCatsMain()}
     * and {@link #importCatsSub()}
     * sets up {@link #categories} and {@link #sub_categories}
     * @see android.os.AsyncTask#doInBackground(Params[])
     */
    @Override
    protected Result doInBackground(Boolean... external) {
      String sourceStr = external[0] ?
          GrisbiSourcesDialogFragment.IMPORT_SOURCE_EXTERNAL :
          GrisbiSourcesDialogFragment.IMPORT_SOURCE_INTERNAL;
      Result r = parseXML(sourceStr);
      if (!r.success) {
        return r;
      }
      setTitle(getString(R.string.grisbi_import_categories_loading,sourceStr));
      phaseChangedP = true;
      setMax(catTree.getTotal());
      publishProgress(0);

      int totalImportedCat = Utils.importCats(catTree,this);
      if (withPartiesP) {
        setTitle(getString(R.string.grisbi_import_parties_loading,sourceStr));
        phaseChangedP = true;
        setMax(partiesList.size());
        publishProgress(0);

        int totalImportedParty = Utils.importParties(partiesList,this);
        return new Result(true,
            R.string.grisbi_import_categories_and_parties_success,
            String.valueOf(totalImportedCat),
            String.valueOf(totalImportedParty));
      } else {
        return new Result(true,
            R.string.grisbi_import_categories_success,
            String.valueOf(totalImportedCat));
      }
    }

    int getMax() {
      return max;
    }
    void setMax(int max) {
      this.max = max;
    }
  }

  public class QifImportTask extends AsyncTask<String, Integer, Result> {
    private int dateFormat;
    private long accountId;

    public QifImportTask(int dateFormat, long accountId) {
      this.dateFormat = dateFormat;
      this.accountId = accountId;
    }

    @Override
    protected void onPostExecute(Result result) {
      if (mCallbacks != null) {
        mCallbacks.onPostExecute(TASK_QIF_IMPORT,result);
      }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
    }

    @Override
    protected Result doInBackground(String... params) {
      return null;
    }
  }
}