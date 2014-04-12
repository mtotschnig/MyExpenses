package org.totschnig.myexpenses.task;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.GrisbiImport;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.util.CategoryTree;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;

import android.content.res.Resources.NotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class GrisbiImportTask extends AsyncTask<Void, Integer, Result> {

  /**
   * 
   */
  private final TaskExecutionFragment taskExecutionFragment;

  public GrisbiImportTask(TaskExecutionFragment taskExecutionFragment, Bundle b) {
    this.taskExecutionFragment = taskExecutionFragment;
    this.withPartiesP = b.getBoolean(TaskExecutionFragment.KEY_WITH_PARTIES);
    this.withCategoriesP = b.getBoolean(TaskExecutionFragment.KEY_WITH_CATEGORIES);
    this.filePath = b.getString(TaskExecutionFragment.KEY_FILE_PATH);
    this.externalP = b.getBoolean(TaskExecutionFragment.KEY_EXTERNAL);
    this.sourceStr = externalP ? filePath :
      this.taskExecutionFragment.getString(R.string.grisbi_import_default_source);
  }

  String title;
  private int max;
  String filePath, sourceStr;
  boolean externalP;
  /**
   * should we handle parties/categories?
   */
  boolean withPartiesP, withCategoriesP;
  /**
   * this is set when we finish one phase (parsing, importing categories,
   * importing parties) so that we can adapt progress dialog in
   * onProgressUpdate
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
   * 
   * @param source2
   */
  protected Result parseXML() {
    InputStream catXML = null;
    Result result;

    try {
      if (externalP) {
        catXML = new FileInputStream(filePath);
      } else {
        int defaultSourceResId = this.taskExecutionFragment.getResources().getIdentifier(
            "cat_"+ Locale.getDefault().getLanguage(),
            "raw",
            this.taskExecutionFragment.getActivity().getPackageName());
        try {
          catXML = this.taskExecutionFragment.getResources()
              .openRawResource(defaultSourceResId);
        } catch (NotFoundException e) {
          catXML = this.taskExecutionFragment.getResources().openRawResource(R.raw.cat_en);
        }
      } 
      result = Utils.analyzeGrisbiFileWithSAX(catXML);
      if (result.success) {
        catTree = (CategoryTree) result.extra[0];
        partiesList = (ArrayList<String>) result.extra[1];
      }
    } catch (FileNotFoundException e) {
      result = new Result(false, R.string.parse_error_file_not_found,
          sourceStr);
    } finally {
      if (catXML != null) {
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
   * made public to allow passing task to
   * {@link Utils#importCats(CategoryTree, GrisbiImportTask)} and
   * {@link Utils#importParties(ArrayList, GrisbiImportTask)}
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
        .getSupportFragmentManager().findFragmentByTag("PROGRESS");
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
    Result r = parseXML();
    if (!r.success) {
      return r;
    }
    setTitle(this.taskExecutionFragment.getString(R.string.grisbi_import_categories_loading, sourceStr));
    phaseChangedP = true;
    setMax(catTree.getTotal());
    publishProgress(0);

    int totalImportedCat,totalImportedParty;
    if (withCategoriesP) {
      totalImportedCat = Utils.importCats(catTree, this);
    } else {
      totalImportedCat = -1;
    }
    if (withPartiesP) {
      setTitle(this.taskExecutionFragment.getString(R.string.grisbi_import_parties_loading, sourceStr));
      phaseChangedP = true;
      setMax(partiesList.size());
      publishProgress(0);
      totalImportedParty = Utils.importParties(partiesList, this);
    } else {
      totalImportedParty = -1;
    }
    return new Result(true,
        0,
        totalImportedCat,
        totalImportedParty);
  }

  int getMax() {
    return max;
  }

  void setMax(int max) {
    this.max = max;
  }
}