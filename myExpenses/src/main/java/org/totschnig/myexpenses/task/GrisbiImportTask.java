package org.totschnig.myexpenses.task;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.db2.Repository;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.util.CategoryTree;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import androidx.core.util.Pair;
import androidx.fragment.app.FragmentActivity;

import static org.totschnig.myexpenses.activity.ProtectedFragmentActivity.PROGRESS_TAG;

import javax.inject.Inject;

public class GrisbiImportTask extends AsyncTask<Void, Integer, Result> {
  private final TaskExecutionFragment taskExecutionFragment;

  public GrisbiImportTask(TaskExecutionFragment taskExecutionFragment, Bundle b) {
    this.taskExecutionFragment = taskExecutionFragment;
    this.withPartiesP = b.getBoolean(TaskExecutionFragment.KEY_WITH_PARTIES);
    this.withCategoriesP = b.getBoolean(TaskExecutionFragment.KEY_WITH_CATEGORIES);
    this.fileUri = b.getParcelable(TaskExecutionFragment.KEY_FILE_PATH);
    this.sourceStr = fileUri.getPath();
    MyApplication.Companion.getInstance().getAppComponent().inject(this);
  }

  private String title;
  private int max;
  private Uri fileUri;
  private String sourceStr;
  /**
   * should we handle parties/categories?
   */
  private boolean withPartiesP, withCategoriesP;
  /**
   * this is set when we finish one phase (parsing, importing categories,
   * importing parties) so that we can adapt progress dialog in
   * onProgressUpdate
   */
  private boolean phaseChangedP = false;
  private CategoryTree catTree;
  private ArrayList<String> partiesList;

  @Inject
  public Repository repository;

  public void setTitle(String title) {
    this.title = title;
  }

  public String getTitle() {
    return title;
  }

  /**
   * return false upon problem (and sets a result object) or true
   */
  protected Result<Pair<CategoryTree, ArrayList<String>>> parseXML() {
    Context app = MyApplication.Companion.getInstance();
    InputStream catXML = null;
    Result<Pair<CategoryTree, ArrayList<String>>> result;

    try {
      catXML = app.getContentResolver().openInputStream(fileUri);
      result = Utils.analyzeGrisbiFileWithSAX(catXML);
      if (result.isSuccess()) {
        catTree = result.getExtra().first;
        partiesList = result.getExtra().second;
      }
    } catch (FileNotFoundException e) {
      result = Result.ofFailure(R.string.parse_error_file_not_found, sourceStr);
    } finally {
      if (catXML != null) {
        try {
          catXML.close();
        } catch (IOException e) {
          CrashHandler.report(e);
        }
      }
    }
    return result;
  }

  /**
   * made public to allow passing task to
   * {@link GrisbiImportHelper#importCats(CategoryTree, GrisbiImportTask)} and
   * {@link Utils#importParties(Repository, ArrayList, GrisbiImportTask)}
   * 
   * @param i
   */
  public void publishProgress(Integer i) {
    super.publishProgress(i);
  }

  /*
   * (non-Javadoc) updates the progress dialog
   * 
   * @see android.os.AsyncTask#onProgressUpdate(Progress[])
   */
  protected void onProgressUpdate(Integer... values) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      if (phaseChangedP) {
        ProgressDialogFragment f = (ProgressDialogFragment) ((FragmentActivity) this.taskExecutionFragment.mCallbacks)
        .getSupportFragmentManager().findFragmentByTag(PROGRESS_TAG);
        if (f!=null) {
          f.setMax(getMax());
          f.setTitle(getTitle());
        }
        phaseChangedP = false;
      }
      this.taskExecutionFragment.mCallbacks.onProgressUpdate(values[0]);
    }
  }

  /*
   * (non-Javadoc) reports on success (with total number of imported
   * categories) or failure
   * 
   * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
   */
  @Override
  protected void onPostExecute(Result result) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onPostExecute(TaskExecutionFragment.TASK_GRISBI_IMPORT, result);
    }
  }

  /*
   * (non-Javadoc) this is where the bulk of the work is done via calls to
   * {@link #importCatsMain()} and {@link #importCatsSub()} sets up {@link
   * #categories} and {@link #sub_categories}
   * 
   * @see android.os.AsyncTask#doInBackground(Params[])
   */
  @Override
  protected Result doInBackground(Void... ignored) {
    final MyApplication application = MyApplication.Companion.getInstance();
    final Context context = application.getWrappedContext();
    Result<Pair<CategoryTree, ArrayList<String>>> r = parseXML();
    if (!r.isSuccess()) {
      return r;
    }
    setTitle(context.getString(R.string.grisbi_import_categories_loading, sourceStr));
    phaseChangedP = true;
    setMax(catTree.getTotal());
    publishProgress(0);

    int totalImportedCat,totalImportedParty;
    if (withCategoriesP) {
      totalImportedCat = GrisbiImportHelper.INSTANCE.importCats(catTree, this);
    } else {
      totalImportedCat = -1;
    }
    if (withPartiesP) {
      setTitle(context.getString(R.string.grisbi_import_parties_loading, sourceStr));
      phaseChangedP = true;
      setMax(partiesList.size());
      publishProgress(0);
      totalImportedParty = Utils.importParties(repository, partiesList, this);
    } else {
      totalImportedParty = -1;
    }
    String msg = "";
    if (totalImportedCat > -1) {
      msg += totalImportedCat == 0 ?
          context.getString(R.string.import_categories_none) :
          context.getString(R.string.import_categories_success, totalImportedCat);
    }
    if (totalImportedParty > -1) {
      if (!TextUtils.isEmpty(msg)) {
        msg += "\n";
      }
      msg += totalImportedParty == 0 ?
          context.getString(R.string.import_parties_none) :
          context.getString(R.string.import_parties_success, totalImportedParty);
    }
    return Result.ofSuccess(msg);
  }

  int getMax() {
    return max;
  }

  void setMax(int max) {
    this.max = max;
  }
}